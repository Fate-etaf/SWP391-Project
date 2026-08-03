package com.swp5.library_management.controller;

import com.swp5.library_management.scheduler.BorrowingNotificationScheduler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Controller cho phép kích hoạt thủ công Scheduler kiểm thử trong môi trường phát triển.
 * KHÔNG dùng trong production.
 *
 * Usage: GET http://localhost:8080/api/test/trigger-borrowing-reminder
 */
@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
@Slf4j
public class TestSchedulerController {

    private final BorrowingNotificationScheduler borrowingNotificationScheduler;
    private final com.swp5.library_management.service.BookReturnService bookReturnService;

    /**
     * Kích hoạt thủ công BorrowingNotificationScheduler để kiểm thử.
     * Không cần chờ đến 00:00 - gọi endpoint này là chạy ngay lập tức.
     */
    @GetMapping("/trigger-borrowing-reminder")
    public ResponseEntity<String> triggerBorrowingReminder() {
        String startTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss dd/MM/yyyy"));
        log.info("[TEST TRIGGER] Kích hoạt thủ công BorrowingNotificationScheduler lúc {}", startTime);

        try {
            borrowingNotificationScheduler.scanAndNotify();
            String message = """
                    ✅ [%s] BorrowingNotificationScheduler đã chạy xong!
                    Kiểm tra Console Log để xem chi tiết số bạn đọc được nhắc nhở.
                    Kiểm tra hộp thư email để xem kết quả gửi mail.
                    """.formatted(startTime);
            return ResponseEntity.ok(message);
        } catch (Exception e) {
            log.error("[TEST TRIGGER] Lỗi khi chạy scheduler: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body("❌ Scheduler chạy thất bại: " + e.getMessage());
        }
    }

    @GetMapping("/check-scan")
    public ResponseEntity<?> testCheckScan(@org.springframework.web.bind.annotation.RequestParam String copyId) {
        try {
            return ResponseEntity.ok(bookReturnService.checkScan(copyId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
