package com.swp5.library_management.dto;

import java.util.Map;

import org.springframework.data.domain.Page;

import lombok.Data;

/**
 * Object tổng hợp toàn bộ dữ liệu trả về cho Thymeleaf để vẽ trang Báo cáo.
 * Sử dụng Generic <T> để tái sử dụng cho nhiều loại bảng (Mượn/Phạt/Luân
 * chuyển).
 */
@Data
public class ReportDataDTO<T> {

    // --- 1. TOP CARDS & INSIGHTS ---
    private long totalRecords;
    private long totalBooksCirculated;
    private double totalFinesCollected;
    private double totalFinesUnpaid;

    // --- 2. BIỂU ĐỒ (Sẵn sàng để đưa vào Chart.js qua th:inline) ---
    // Line Chart (Lưu lượng giao dịch theo ngày): Map<"yyyy-MM-dd", Số lượng>
    private Map<String, Long> dailyTransactionChart;

    // Doughnut Chart (Tỷ lệ thu hồi nợ phạt): Map<"Paid"/"Unpaid", Số tiền>
    private Map<String, Double> fineRecoveryChart;

    // Bar Chart (Cơ cấu vi phạm): Map<"Overdue"/"Lost"/"Damaged", Số lượng>
    private Map<String, Long> violationStructureChart;

    // --- 3. BẢNG DỮ LIỆU ---
    // Sử dụng đối tượng Page của Spring để Thymeleaf dễ dàng làm thanh Pagination
    private Page<T> tableData;

    // Cờ báo hiệu giao diện hiển thị banner cảnh báo (Hard Limit > 1000)
    private boolean isHardLimited;
}