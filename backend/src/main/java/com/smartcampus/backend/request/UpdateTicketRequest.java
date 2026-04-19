package com.smartcampus.backend.request;

import com.smartcampus.backend.enums.TicketStatus;
import lombok.Data;

@Data
public class UpdateTicketRequest {

    private TicketStatus status;

    private Long assignedToId;

    private String resolutionNotes;

    private String rejectionReason;
}
