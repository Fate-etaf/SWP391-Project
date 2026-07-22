package com.swp5.library_management.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO đại diện cho tình trạng hoàn thành nghĩa vụ thư viện của một sinh viên.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GraduationStatusDTO {
    private String studentId;
    private String fullName;
    private boolean cleared; // True nếu không còn nợ phí và không còn mượn sách
    private int borrowingCount; // Số sách đang mượn chưa trả
    private BigDecimal totalUnpaidFine;
    private String message; // Chi tiết tình trạng
}
