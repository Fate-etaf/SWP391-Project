package com.swp5.library_management.reader.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO chứa dữ liệu thống kê tổng quan hiển thị ở phần Metrics trên trang chủ.
 * Tách riêng khỏi Entity để View không phụ thuộc vào cấu trúc DB.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HomeStatsDTO {

    /** Tổng số đầu sách duy nhất trong hệ thống */
    private long totalBooks;

    /** Số bản sao đang ở trạng thái "Available" (sẵn sàng cho mượn) */
    private long availableCopies;

    /** Số bạn đọc tích cực */
    private long activeReaders;

    /** Tổng số cơ sở đang kết nối vào hệ thống */
    private long totalCampuses;
}
