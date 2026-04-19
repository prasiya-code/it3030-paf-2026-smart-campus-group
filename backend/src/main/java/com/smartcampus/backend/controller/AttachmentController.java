package com.smartcampus.backend.controller;

import com.smartcampus.backend.entity.Attachment;
import com.smartcampus.backend.service.AttachmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import com.smartcampus.backend.repository.UserRepository;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequestMapping("/api/attachments")
public class AttachmentController {

    @Autowired
    private AttachmentService attachmentService;

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/upload/{ticketId}")
    public ResponseEntity<Attachment> uploadAttachment(@PathVariable Long ticketId,
                                                       @RequestParam("file") MultipartFile file,
                                                       Authentication authentication) throws IOException {
        Long userId = extractUserId(authentication);
        Attachment attachment = attachmentService.uploadAttachment(ticketId, file, userId);
        return new ResponseEntity<>(attachment, HttpStatus.CREATED);
    }

    @GetMapping("/ticket/{ticketId}")
    public ResponseEntity<List<Attachment>> getAttachmentsByTicket(@PathVariable Long ticketId) {
        return ResponseEntity.ok(attachmentService.getAttachmentsByTicket(ticketId));
    }

    @GetMapping("/download/{id}")
    public ResponseEntity<Resource> downloadAttachment(@PathVariable Long id) {
        Attachment attachment = attachmentService.getAttachmentById(id);
        try {
            Path filePath = Paths.get(attachment.getFilePath());
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() || resource.isReadable()) {
                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(attachment.getContentType()))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + attachment.getFileName() + "\"")
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAttachment(@PathVariable Long id,
                                              Authentication authentication) {
        Long userId = extractUserId(authentication);
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
        attachmentService.deleteAttachment(id, userId, isAdmin);
        return ResponseEntity.noContent().build();
    }

    private Long extractUserId(Authentication authentication) {
        if (authentication == null) {
            return 1L; // temporary test user id
        }
        if (authentication.getPrincipal() instanceof OAuth2User) {
            return ((OAuth2User) authentication.getPrincipal()).getAttribute("id");
        } else if (authentication.getPrincipal() instanceof org.springframework.security.core.userdetails.UserDetails) {
            String email = ((org.springframework.security.core.userdetails.UserDetails) authentication.getPrincipal()).getUsername();
            com.smartcampus.backend.entity.User user = userRepository.findByEmail(email).orElse(null);
            return user != null ? user.getId() : 1L;
        }
        return 1L;
    }
}
