package com.smartcampus.backend.controller;

import com.smartcampus.backend.entity.Attachment;
import com.smartcampus.backend.service.AttachmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/attachments")
public class AttachmentController {

    @Autowired
    private AttachmentService attachmentService;

    @PostMapping("/upload/{ticketId}")
    public ResponseEntity<Attachment> uploadAttachment(@PathVariable Long ticketId,
                                                       @RequestParam("file") MultipartFile file,
                                                       @AuthenticationPrincipal OAuth2User principal) throws IOException {
        Long userId = extractUserId(principal);
        Attachment attachment = attachmentService.uploadAttachment(ticketId, file, userId);
        return new ResponseEntity<>(attachment, HttpStatus.CREATED);
    }

    @GetMapping("/ticket/{ticketId}")
    public ResponseEntity<List<Attachment>> getAttachmentsByTicket(@PathVariable Long ticketId) {
        return ResponseEntity.ok(attachmentService.getAttachmentsByTicket(ticketId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAttachment(@PathVariable Long id,
                                              @AuthenticationPrincipal OAuth2User principal) {
        Long userId = extractUserId(principal);
        boolean isAdmin = principal.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
        attachmentService.deleteAttachment(id, userId, isAdmin);
        return ResponseEntity.noContent().build();
    }

    private Long extractUserId(OAuth2User principal) {
        if (principal == null) {
            return 1L; // temporary test user id (must exist in DB)
        }
        return principal.getAttribute("id");
    }
}
