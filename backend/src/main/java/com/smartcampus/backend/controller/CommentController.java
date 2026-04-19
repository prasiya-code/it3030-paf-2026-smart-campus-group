package com.smartcampus.backend.controller;

import com.smartcampus.backend.entity.Comment;
import com.smartcampus.backend.request.CreateCommentRequest;
import com.smartcampus.backend.service.CommentService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;
import com.smartcampus.backend.repository.UserRepository;

import java.util.List;

@RestController
@RequestMapping("/api/comments")
public class CommentController {

    @Autowired
    private CommentService commentService;

    @Autowired
    private UserRepository userRepository;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Comment> createComment(@Valid @RequestBody CreateCommentRequest request,
                                                Authentication authentication) {
        Long userId = extractUserId(authentication);
        Comment comment = commentService.createComment(request, userId);
        return new ResponseEntity<>(comment, HttpStatus.CREATED);
    }

    @GetMapping("/ticket/{ticketId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Comment>> getCommentsByTicket(@PathVariable Long ticketId) {
        return ResponseEntity.ok(commentService.getCommentsByTicket(ticketId));
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Comment> updateComment(@PathVariable Long id,
                                                  @RequestBody String newContent,
                                                  Authentication authentication) {
        Long userId = extractUserId(authentication);
        return ResponseEntity.ok(commentService.updateComment(id, newContent, userId));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteComment(@PathVariable Long id,
                                            Authentication authentication) {
        Long userId = extractUserId(authentication);
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
        commentService.deleteComment(id, userId, isAdmin);
        return ResponseEntity.noContent().build();
    }

    private Long extractUserId(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return 1L; // fallback for unauthenticated (should be blocked by security)
        }
        
        Object principal = authentication.getPrincipal();

        if (principal instanceof OAuth2User oauth2User) {
            Object idAttr = oauth2User.getAttribute("id");

            if (idAttr != null) {
                return Long.valueOf(idAttr.toString());
            }

            // Fallback for old sessions that don't have "id" in attributes
            String oauthEmail = oauth2User.getAttribute("email");
            if (oauthEmail != null) {
                return userRepository.findByEmail(oauthEmail)
                        .map(user -> user.getId() != null ? user.getId() : 1L)
                        .orElse(1L);
            }
            return 1L;
        }
        
        // Manual login uses email as username
        String email = authentication.getName();
        if (email == null) return 1L;
        return userRepository.findByEmail(email)
                .map(user -> user.getId() != null ? user.getId() : 1L)
                .orElse(1L);
    }
}
