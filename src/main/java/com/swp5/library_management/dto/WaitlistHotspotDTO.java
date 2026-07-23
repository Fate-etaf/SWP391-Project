package com.swp5.library_management.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WaitlistHotspotDTO {
    private Integer bookId;
    private String title;
    private String isbn;
    private Integer campusId; // Cơ sở đang khát sách
    private String campusName;
    private long waitingCount;
}