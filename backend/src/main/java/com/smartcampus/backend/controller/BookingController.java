package com.smartcampus.backend.controller;

import com.smartcampus.backend.entity.Booking;
import com.smartcampus.backend.entity.User;
import com.smartcampus.backend.enums.BookingStatus;
import com.smartcampus.backend.repository.UserRepository;
import com.smartcampus.backend.request.CreateBookingRequest;
import com.smartcampus.backend.request.UpdateBookingRequest;
import com.smartcampus.backend.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private UserRepository userRepository;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Booking> createBooking(
            @Valid @RequestBody CreateBookingRequest request,
            Authentication authentication
    ) {
        Long userId = extractUserId(authentication);
        Booking booking = bookingService.createBooking(request, userId);
        return new ResponseEntity<>(booking, HttpStatus.CREATED);
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Booking>> getAllBookings(
            @RequestParam(required = false) BookingStatus status,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long resourceId,
            Authentication authentication
    ) {
        Long authUserId = extractUserId(authentication);
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(auth -> "ROLE_ADMIN".equals(auth.getAuthority()));

        if (!isAdmin && userId != null && !userId.equals(authUserId)) {
            throw new AccessDeniedException("You can only view your own bookings");
        }

        if (!isAdmin && userId == null) {
            userId = authUserId;
        }

        return ResponseEntity.ok(
                bookingService.getBookingsWithFilters(status, userId, resourceId)
        );
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Booking>> getBookingsByUser(
            @PathVariable Long userId,
            Authentication authentication
    ) {
        Long authUserId = extractUserId(authentication);
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(auth -> "ROLE_ADMIN".equals(auth.getAuthority()));

        if (!isAdmin && !userId.equals(authUserId)) {
            throw new AccessDeniedException("You can only view your own bookings");
        }

        return ResponseEntity.ok(bookingService.getBookingsByUser(userId));
    }

    @GetMapping("/my-bookings")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Booking>> getMyBookings(Authentication authentication) {
        Long userId = extractUserId(authentication);
        return ResponseEntity.ok(bookingService.getBookingsByUser(userId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Booking> getBookingById(
            @PathVariable Long id,
            Authentication authentication
    ) {
        Booking booking = bookingService.getBookingById(id);
        Long authUserId = extractUserId(authentication);
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(auth -> "ROLE_ADMIN".equals(auth.getAuthority()));

        if (!isAdmin && !booking.getUser().getId().equals(authUserId)) {
            throw new AccessDeniedException("You can only view your own bookings");
        }

        return ResponseEntity.ok(booking);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Booking> updateBooking(
            @PathVariable Long id,
            @Valid @RequestBody UpdateBookingRequest request,
            Authentication authentication
    ) {
        Long adminId = extractUserId(authentication);
        return ResponseEntity.ok(bookingService.updateBookingStatus(id, request, adminId));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Booking> cancelBooking(
            @PathVariable Long id,
            Authentication authentication
    ) {
        Long userId = extractUserId(authentication);
        return ResponseEntity.ok(bookingService.cancelBooking(id, userId));
    }

    private Long extractUserId(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new RuntimeException("User not authenticated properly");
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof OAuth2User oauth2User) {
            Object idAttr = oauth2User.getAttribute("id");

            if (idAttr != null) {
                return Long.valueOf(idAttr.toString());
            }

            String email = oauth2User.getAttribute("email");
            if (email == null || email.isBlank()) {
                throw new RuntimeException("OAuth2 user email not found");
            }

            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            return user.getId();
        }

        if (principal instanceof UserDetails userDetails) {
            String email = userDetails.getUsername();

            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            return user.getId();
        }

        if (principal instanceof String principalString) {
            if ("anonymousUser".equals(principalString)) {
                throw new RuntimeException("Anonymous user is not allowed");
            }

            User user = userRepository.findByEmail(principalString)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            return user.getId();
        }

        throw new RuntimeException("Unsupported authentication type");
    }
}