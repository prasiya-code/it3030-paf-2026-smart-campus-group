package com.smartcampus.backend.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.smartcampus.backend.enums.BookingStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class UpdateBookingRequest {

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate bookingDate;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime startTime;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime endTime;

    private String purpose;

    private Integer expectedAttendees;

    @NotNull(message = "Status is required")
    private BookingStatus status;

    private String adminReason;
}
