package com.smartcampus.backend.controller;

import com.smartcampus.backend.entity.Booking;
import com.smartcampus.backend.enums.BookingStatus;
import com.smartcampus.backend.request.CreateBookingRequest;
import com.smartcampus.backend.request.UpdateBookingRequest;
import com.smartcampus.backend.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    @Autowired
    private BookingService bookingService;

    @PostMapping
    public ResponseEntity<Booking> createBooking(@Valid @RequestBody CreateBookingRequest request,
                                                 @AuthenticationPrincipal OAuth2User principal) {
        Long userId = extractUserId(principal);
        Booking booking = bookingService.createBooking(request, userId);
        return new ResponseEntity<>(booking, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<Booking>> getAllBookings(
            @RequestParam(required = false) BookingStatus status,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long resourceId) {
        return ResponseEntity.ok(bookingService.getBookingsWithFilters(status, userId, resourceId));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Booking>> getBookingsByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(bookingService.getBookingsByUser(userId));
    }

    @GetMapping("/my-bookings")
    public ResponseEntity<List<Booking>> getMyBookings(@AuthenticationPrincipal OAuth2User principal) {
        Long userId = extractUserId(principal);
        return ResponseEntity.ok(bookingService.getBookingsByUser(userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Booking> getBookingById(@PathVariable Long id) {
        return ResponseEntity.ok(bookingService.getBookingById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Booking> updateBooking(@PathVariable Long id,
                                                 @Valid @RequestBody UpdateBookingRequest request,
                                                 @AuthenticationPrincipal OAuth2User principal) {
        Long adminId = extractUserId(principal);
        return ResponseEntity.ok(bookingService.updateBookingStatus(id, request, adminId));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<Booking> cancelBooking(@PathVariable Long id,
                                                 @AuthenticationPrincipal OAuth2User principal) {
        Long userId = extractUserId(principal);
        return ResponseEntity.ok(bookingService.cancelBooking(id, userId));
    }

    private Long extractUserId(OAuth2User principal) {
        if (principal == null) {
            return 1L; // temporary test user id (must exist in DB)
        }
        return principal.getAttribute("id");
    }
}
