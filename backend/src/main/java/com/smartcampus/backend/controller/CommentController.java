package com.smartcampus.backend.controller;

import com.smartcampus.backend.entity.Comment;
import com.smartcampus.backend.request.CreateCommentRequest;
import com.smartcampus.backend.service.CommentService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/comments")
public class CommentController {

    @Autowired
    private CommentService commentService;

    @PostMapping
    public ResponseEntity<Comment> createComment(@Valid @RequestBody CreateCommentRequest request,
                                                @AuthenticationPrincipal OAuth2User principal) {
        Long userId = extractUserId(principal);
        Comment comment = commentService.createComment(request, userId);
        return new ResponseEntity<>(comment, HttpStatus.CREATED);
    }

    @GetMapping("/ticket/{ticketId}")
    public ResponseEntity<List<Comment>> getCommentsByTicket(@PathVariable Long ticketId) {
        return ResponseEntity.ok(commentService.getCommentsByTicket(ticketId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Comment> updateComment(@PathVariable Long id,
                                                  @RequestBody String newContent,
                                                  @AuthenticationPrincipal OAuth2User principal) {
        Long userId = extractUserId(principal);
        return ResponseEntity.ok(commentService.updateComment(id, newContent, userId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteComment(@PathVariable Long id,
                                           @AuthenticationPrincipal OAuth2User principal) {
        Long userId = extractUserId(principal);
        boolean isAdmin = principal.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
        commentService.deleteComment(id, userId, isAdmin);
        return ResponseEntity.noContent().build();
    }

    private Long extractUserId(OAuth2User principal) {
        if (principal == null) {
            return 1L; // temporary test user id (must exist in DB)
        }
        return principal.getAttribute("id");
    }
}
