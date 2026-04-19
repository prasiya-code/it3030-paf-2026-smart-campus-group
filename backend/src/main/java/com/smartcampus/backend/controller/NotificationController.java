package com.smartcampus.backend.controller;

import com.smartcampus.backend.entity.Notification;
import com.smartcampus.backend.entity.User;
import com.smartcampus.backend.repository.UserRepository;
import com.smartcampus.backend.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private UserRepository userRepository;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Notification>> getMyNotifications(Authentication authentication) {
        Long userId = extractUserId(authentication);
        return ResponseEntity.ok(notificationService.getUserNotifications(userId));
    }

    @GetMapping("/unread")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Notification>> getUnreadNotifications(Authentication authentication) {
        Long userId = extractUserId(authentication);
        return ResponseEntity.ok(notificationService.getUnreadNotifications(userId));
    }

    @GetMapping("/count")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Long>> getUnreadCount(Authentication authentication) {
        Long userId = extractUserId(authentication);
        Map<String, Long> response = new HashMap<>();
        response.put("count", notificationService.getUnreadCount(userId));
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/read")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> markAsRead(@PathVariable Long id, Authentication authentication) {
        Long userId = extractUserId(authentication);
        notificationService.markAsRead(id, userId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/read-all")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> markAllAsRead(Authentication authentication) {
        Long userId = extractUserId(authentication);
        notificationService.markAllAsRead(userId);
        return ResponseEntity.ok().build();
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

        if (principal instanceof org.springframework.security.core.userdetails.UserDetails userDetails) {
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
