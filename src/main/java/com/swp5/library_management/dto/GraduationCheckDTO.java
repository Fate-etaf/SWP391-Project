package com.swp5.library_management.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GraduationCheckDTO {

    private String studentId;
    private String fullName;
    private String email;

    /** Sinh viên có tồn tại trong hệ thống không */
    private boolean existsInSystem;

    /** Sinh viên đã hoàn thành mọi nghĩa vụ chưa */
    private boolean cleared;

    // Chi tiết lỗi cụ thể của dòng này
    private String copyId;
    private String bookTitle;
    private String reason;
    private java.math.BigDecimal remainingAmount;
}
