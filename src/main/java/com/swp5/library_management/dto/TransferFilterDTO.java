package com.swp5.library_management.dto;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
public class TransferFilterDTO {
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    private Integer fromCampusId;
    private Integer toCampusId;
    private String copyId;
    private String status;

    private int page = 0;
    private int size = 50;

    public void initDefaultDatesIfNull() {
        if (this.startDate == null) {
            this.startDate = LocalDate.now().minusDays(30);
        }
        if (this.endDate == null) {
            this.endDate = LocalDate.now();
        }
    }
}
