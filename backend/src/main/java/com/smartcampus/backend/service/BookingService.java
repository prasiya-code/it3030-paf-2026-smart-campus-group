package com.smartcampus.backend.service;

import com.smartcampus.backend.entity.Booking;
import com.smartcampus.backend.entity.Resource;
import com.smartcampus.backend.entity.User;
import com.smartcampus.backend.enums.BookingStatus;
import com.smartcampus.backend.enums.NotificationType;
import com.smartcampus.backend.enums.ResourceStatus;
import com.smartcampus.backend.exception.BadRequestException;
import com.smartcampus.backend.exception.ResourceNotFoundException;
import com.smartcampus.backend.repository.BookingRepository;
import com.smartcampus.backend.repository.ResourceRepository;
import com.smartcampus.backend.repository.UserRepository;
import com.smartcampus.backend.request.CreateBookingRequest;
import com.smartcampus.backend.request.UpdateBookingRequest;
import com.smartcampus.backend.request.UserUpdateBookingRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class BookingService {

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private ResourceRepository resourceRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationService notificationService;

    @Transactional
    public Booking createBooking(CreateBookingRequest request, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Resource resource = resourceRepository.findById(request.getResourceId())
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found"));

        if (resource.getStatus() == ResourceStatus.OUT_OF_SERVICE) {
            throw new BadRequestException("Resource is currently out of service");
        }

        if (request.getStartTime().isAfter(request.getEndTime()) ||
                request.getStartTime().equals(request.getEndTime())) {
            throw new BadRequestException("End time must be after start time");
        }

        boolean hasConflict = bookingRepository.hasOverlappingBooking(
                request.getResourceId(),
                request.getBookingDate(),
                request.getStartTime(),
                request.getEndTime()
        );

        if (hasConflict) {
            throw new BadRequestException("Resource is not available for the selected time slot");
        }

        Booking booking = new Booking();
        booking.setBookingCode("BK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        booking.setUser(user);
        booking.setResource(resource);
        booking.setBookingDate(request.getBookingDate());
        booking.setStartTime(request.getStartTime());
        booking.setEndTime(request.getEndTime());
        booking.setPurpose(request.getPurpose());
        booking.setExpectedAttendees(request.getExpectedAttendees());
        booking.setStatus(BookingStatus.PENDING);

        return bookingRepository.save(booking);
    }

    public List<Booking> getAllBookings() {
        return bookingRepository.findAll();
    }

    public Booking getBookingById(Long id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
    }

    public List<Booking> getBookingsByUser(Long userId) {
        return bookingRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public List<Booking> getBookingsWithFilters(BookingStatus status, Long userId, Long resourceId) {
        return bookingRepository.findBookingsWithFilters(status, userId, resourceId);
    }

    @Transactional
    public Booking updateBookingStatus(Long id, UpdateBookingRequest request, Long adminId) {
        Booking booking = getBookingById(id);

        if (request.getStatus() == null) {
            throw new BadRequestException("Status is required");
        }

        booking.setStatus(request.getStatus());

        if (request.getAdminReason() != null) {
            booking.setAdminReason(request.getAdminReason());
        }

        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin user not found"));

        booking.setApprovedBy(admin);
        booking.setApprovedAt(LocalDateTime.now());

        Booking updated = bookingRepository.save(booking);

        if (request.getStatus() == BookingStatus.APPROVED) {
            notificationService.createNotification(
                    booking.getUser().getId(),
                    "Booking Approved",
                    "Your booking for " + booking.getResource().getName() + " has been approved.",
                    NotificationType.BOOKING_APPROVED,
                    booking.getId(),
                    null
            );
        } else if (request.getStatus() == BookingStatus.REJECTED) {
            notificationService.createNotification(
                    booking.getUser().getId(),
                    "Booking Rejected",
                    "Your booking for " + booking.getResource().getName() + " has been rejected. Reason: "
                            + (request.getAdminReason() != null ? request.getAdminReason() : "No reason provided"),
                    NotificationType.BOOKING_REJECTED,
                    booking.getId(),
                    null
            );
        }

        return updated;
    }

    @Transactional
    public Booking cancelBooking(Long id, Long userId) {
        Booking booking = getBookingById(id);

        if (!booking.getUser().getId().equals(userId)) {
            throw new BadRequestException("You can only cancel your own bookings");
        }

        if (booking.getStatus() != BookingStatus.PENDING && booking.getStatus() != BookingStatus.APPROVED) {
            throw new BadRequestException("Only pending or approved bookings can be cancelled");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        Booking cancelled = bookingRepository.save(booking);

        notificationService.createNotification(
                booking.getUser().getId(),
                "Booking Cancelled",
                "Your booking for " + booking.getResource().getName() + " has been cancelled.",
                NotificationType.BOOKING_CANCELLED,
                booking.getId(),
                null
        );

        return cancelled;
    }

    @Transactional
    public Booking updateUserBooking(Long id, UserUpdateBookingRequest request, Long userId) {
        Booking booking = getBookingById(id);

        if (!booking.getUser().getId().equals(userId)) {
            throw new BadRequestException("You can only update your own bookings");
        }

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new BadRequestException("Only PENDING bookings can be edited");
        }

        Long resourceId = booking.getResource().getId();

        if (request.getBookingDate() != null) {
            booking.setBookingDate(request.getBookingDate());
        }
        if (request.getStartTime() != null) {
            booking.setStartTime(request.getStartTime());
        }
        if (request.getEndTime() != null) {
            booking.setEndTime(request.getEndTime());
        }
        if (request.getPurpose() != null) {
            booking.setPurpose(request.getPurpose());
        }
        if (request.getExpectedAttendees() != null) {
            booking.setExpectedAttendees(request.getExpectedAttendees());
        }

        if (booking.getStartTime().isAfter(booking.getEndTime()) ||
                booking.getStartTime().equals(booking.getEndTime())) {
            throw new BadRequestException("End time must be after start time");
        }

        boolean hasConflict = bookingRepository.hasOverlappingBooking(
                resourceId,
                booking.getBookingDate(),
                booking.getStartTime(),
                booking.getEndTime(),
                booking.getId()
        );

        if (hasConflict) {
            throw new BadRequestException("Resource is not available for the updated time slot");
        }

        return bookingRepository.save(booking);
    }
}