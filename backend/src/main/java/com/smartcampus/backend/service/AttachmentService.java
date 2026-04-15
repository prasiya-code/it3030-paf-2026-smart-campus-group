package com.smartcampus.backend.service;

import com.smartcampus.backend.entity.Attachment;
import com.smartcampus.backend.entity.Ticket;
import com.smartcampus.backend.exception.BadRequestException;
import com.smartcampus.backend.exception.ResourceNotFoundException;
import com.smartcampus.backend.repository.AttachmentRepository;
import com.smartcampus.backend.repository.TicketRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Service
public class AttachmentService {

    @Autowired
    private AttachmentRepository attachmentRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    @Transactional
    public Attachment uploadAttachment(Long ticketId, MultipartFile file, Long userId) throws IOException {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found"));

        // Verify user is ticket creator or admin
        if (!ticket.getCreatedBy().getId().equals(userId)) {
            throw new BadRequestException("Only ticket creator can add attachments");
        }

        // Check if ticket already has 3 attachments
        List<Attachment> existingAttachments = attachmentRepository.findByTicketId(ticketId);
        if (existingAttachments.size() >= 3) {
            throw new BadRequestException("Maximum 3 attachments allowed per ticket");
        }

        // Validate file type (images only)
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BadRequestException("Only image files are allowed");
        }

        // Create upload directory if it doesn't exist
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Generate unique filename
        String originalFilename = file.getOriginalFilename();
        String fileExtension = originalFilename != null ? 
                originalFilename.substring(originalFilename.lastIndexOf(".")) : ".jpg";
        String uniqueFilename = UUID.randomUUID() + fileExtension;

        // Save file to disk
        Path filePath = uploadPath.resolve(uniqueFilename);
        Files.copy(file.getInputStream(), filePath);

        // Save attachment record
        Attachment attachment = new Attachment();
        attachment.setTicket(ticket);
        attachment.setFileName(originalFilename);
        attachment.setFilePath(filePath.toString());
        attachment.setContentType(contentType);
        attachment.setFileSize(file.getSize());

        return attachmentRepository.save(attachment);
    }

    public List<Attachment> getAttachmentsByTicket(Long ticketId) {
        return attachmentRepository.findByTicketId(ticketId);
    }

    @Transactional
    public void deleteAttachment(Long attachmentId, Long userId, boolean isAdmin) {
        Attachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Attachment not found"));

        if (!attachment.getTicket().getCreatedBy().getId().equals(userId) && !isAdmin) {
            throw new BadRequestException("You can only delete your own attachments or must be an admin");
        }

        // Delete file from disk
        try {
            Path filePath = Paths.get(attachment.getFilePath());
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            // Log error but continue with database deletion
        }

        attachmentRepository.delete(attachment);
    }
}
