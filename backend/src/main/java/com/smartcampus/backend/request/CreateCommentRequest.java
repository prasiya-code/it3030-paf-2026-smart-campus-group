package com.smartcampus.backend.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateCommentRequest {

    @NotNull(message = "Ticket ID is required")
    private Long ticketId;

    @NotBlank(message = "Content is required")
    private String content;
}
