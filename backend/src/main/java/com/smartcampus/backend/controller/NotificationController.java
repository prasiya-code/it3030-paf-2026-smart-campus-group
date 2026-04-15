package com.smartcampus.backend.controller;

import com.smartcampus.backend.entity.Notification;
import com.smartcampus.backend.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @GetMapping
    public ResponseEntity<List<Notification>> getMyNotifications(@AuthenticationPrincipal OAuth2User principal) {
        Long userId = extractUserId(principal);
        return ResponseEntity.ok(notificationService.getUserNotifications(userId));
    }

    @GetMapping("/unread")
    public ResponseEntity<List<Notification>> getUnreadNotifications(@AuthenticationPrincipal OAuth2User principal) {
        Long userId = extractUserId(principal);
        return ResponseEntity.ok(notificationService.getUnreadNotifications(userId));
    }

    @GetMapping("/count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(@AuthenticationPrincipal OAuth2User principal) {
        Long userId = extractUserId(principal);
        Map<String, Long> response = new HashMap<>();
        response.put("count", notificationService.getUnreadCount(userId));
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long id,
                                           @AuthenticationPrincipal OAuth2User principal) {
        Long userId = extractUserId(principal);
        notificationService.markAsRead(id, userId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(@AuthenticationPrincipal OAuth2User principal) {
        Long userId = extractUserId(principal);
        notificationService.markAllAsRead(userId);
        return ResponseEntity.ok().build();
    }

    private Long extractUserId(OAuth2User principal) {
        if (principal == null) {
            return 1L; // temporary test user id (must exist in DB)
        }
        return principal.getAttribute("id");
    }
}
