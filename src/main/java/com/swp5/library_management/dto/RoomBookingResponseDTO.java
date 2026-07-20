package com.swp5.library_management.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomBookingResponseDTO {
    private Integer bookingId;
    private String roomName;
    private String patronId;
    private LocalDate bookingDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer participantCount;
    private String purpose;
    private String status;
    private LocalDateTime createdAt;
    private String qrCodeBase64;
}
