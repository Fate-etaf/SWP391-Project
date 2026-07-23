package com.swp5.library_management.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoomBookingRequestDTO {
    private Integer roomId;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate bookingDate;

    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime startTime;

    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime endTime;

    private Integer participantCount;
    private String purpose;
}
