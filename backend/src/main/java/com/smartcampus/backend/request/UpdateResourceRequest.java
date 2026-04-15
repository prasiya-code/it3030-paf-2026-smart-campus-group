package com.smartcampus.backend.request;

import com.smartcampus.backend.enums.ResourceStatus;
import com.smartcampus.backend.enums.ResourceType;
import jakarta.validation.constraints.Min;
import lombok.Data;
import java.time.LocalTime;

@Data
public class UpdateResourceRequest {

    private String name;

    private ResourceType type;

    private String location;

    @Min(value = 1, message = "Capacity must be at least 1")
    private Integer capacity;

    private String description;

    private ResourceStatus status;

    private LocalTime availabilityStart;

    private LocalTime availabilityEnd;
}
