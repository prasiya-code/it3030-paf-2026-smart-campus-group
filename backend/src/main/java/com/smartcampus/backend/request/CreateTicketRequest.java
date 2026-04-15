package com.smartcampus.backend.request;

import com.smartcampus.backend.enums.TicketCategory;
import com.smartcampus.backend.enums.TicketPriority;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateTicketRequest {

    private Long resourceId;

    @NotNull(message = "Category is required")
    private TicketCategory category;

    @NotNull(message = "Priority is required")
    private TicketPriority priority;

    private String description;

    private String location;

    private String preferredContactName;

    private String preferredContactEmail;

    private String preferredContactPhone;
}
