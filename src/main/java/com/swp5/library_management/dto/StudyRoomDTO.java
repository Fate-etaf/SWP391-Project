package com.swp5.library_management.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudyRoomDTO {
    private Integer roomId;
    private String roomName;
    private Integer capacity;
    private String description;
    private String status;
    private List<BookedSlotDTO> bookedSlots;
}
