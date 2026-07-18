package com.swp5.library_management.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportSummaryDTO {
    private long totalBorrowed;
    private long totalReturned;
    private long totalOverdue;
    private BigDecimal totalFinesCollected;
    private BigDecimal totalFinesPending;
}