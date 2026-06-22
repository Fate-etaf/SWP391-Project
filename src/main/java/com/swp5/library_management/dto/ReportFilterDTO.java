package com.swp5.library_management.dto;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

import lombok.Data;

/**
 * Object hứng toàn bộ dữ liệu từ Form Lọc (GET request) của Thymeleaf.
 */
@Data
public class ReportFilterDTO {

    // Master Switch: Quyết định bảng dữ liệu (BORROW, FINE, TRANSFER)
    private String reportType = "BORROW";

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    private Integer campusId;
    private String copyId;
    private String userId; // Dùng chung cho PatronID hoặc LibrarianID
    private String status;

    // Tham số phân trang (Mặc định trang 0, 50 dòng/trang)
    private int page = 0;
    private int size = 50;

    // Helper: Khởi tạo giá trị thời gian mặc định (Ví dụ: 30 ngày gần nhất) nếu
    // user chưa chọn
    public void initDefaultDatesIfNull() {
        if (this.startDate == null) {
            this.startDate = LocalDate.now().minusDays(30);
        }
        if (this.endDate == null) {
            this.endDate = LocalDate.now();
        }
    }
}