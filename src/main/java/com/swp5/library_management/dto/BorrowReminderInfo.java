package com.swp5.library_management.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * DTO chứa thông tin 1 cuốn sách cần nhắc nhở hạn trả.
 * Dùng trong BorrowingNotificationScheduler và BorrowReminderEmailService.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BorrowReminderInfo {

    /** Tên cuốn sách */
    private String bookTitle;

    /** Mã bản sao vật lý */
    private String copyId;

    /** Ngày hạn trả sách */
    private LocalDateTime dueDate;

    /**
     * Trạng thái hiển thị trong email, ví dụ:
     *   - "Sắp hết hạn (còn 1 ngày)"
     *   - "Đã quá hạn 3 ngày"
     */
    private String statusLabel;

    /** true nếu sách đã quá hạn, false nếu chỉ sắp hết hạn */
    private boolean overdue;
}
