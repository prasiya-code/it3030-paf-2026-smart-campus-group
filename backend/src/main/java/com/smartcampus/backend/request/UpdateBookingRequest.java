package com.smartcampus.backend.request;

import com.smartcampus.backend.enums.BookingStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class UpdateBookingRequest {

    private LocalDate bookingDate;

    private LocalTime startTime;

    private LocalTime endTime;

    private String purpose;

    private Integer expectedAttendees;

    @NotNull(message = "Status is required")
    private BookingStatus status;

    private String adminReason;
}
