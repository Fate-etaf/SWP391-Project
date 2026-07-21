package com.swp5.library_management.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookedSlotDTO {
    private LocalTime startTime;
    private LocalTime endTime;
    private boolean isOwnBooking;
}
