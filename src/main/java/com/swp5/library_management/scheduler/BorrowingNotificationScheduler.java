package com.swp5.library_management.scheduler;

import com.swp5.library_management.dto.BorrowReminderInfo;
import com.swp5.library_management.entity.BorrowTicketDetail;
import com.swp5.library_management.entity.User;
import com.swp5.library_management.repository.BorrowTicketDetailRepository;
import com.swp5.library_management.repository.NotificationRepository;
import com.swp5.library_management.service.BorrowReminderEmailService;
import com.swp5.library_management.entity.Notification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Scheduler tự động quét sách đang mượn vào lúc 00:00 hàng đêm.
 * Gửi email nhắc nhở cho bạn đọc nếu sách sắp hết hạn hoặc đã quá hạn.
 *
 * Thời gian và mốc cảnh báo được cấu hình linh hoạt trong application.properties:
 *   app.scheduler.borrow-reminder.cron      = cron expression (mặc định 0 0 0 * * ?)
 *   app.scheduler.borrow-reminder.days-before = số ngày cảnh báo trước hạn (mặc định 1)
 */
@Component
@Slf4j
public class BorrowingNotificationScheduler {

    private final BorrowTicketDetailRepository borrowTicketDetailRepository;
    private final BorrowReminderEmailService borrowReminderEmailService;
    private final NotificationRepository notificationRepository;

    @Value("${app.scheduler.borrow-reminder.days-before:1}")
    private int daysBefore;

    private static final java.util.regex.Pattern EMAIL_PATTERN = java.util.regex.Pattern.compile(
        "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$"
    );

    private boolean isValidEmail(String email) {
        if (email == null) return false;
        return EMAIL_PATTERN.matcher(email).matches();
    }

    public BorrowingNotificationScheduler(BorrowTicketDetailRepository borrowTicketDetailRepository,
                                          BorrowReminderEmailService borrowReminderEmailService,
                                          NotificationRepository notificationRepository) {
        this.borrowTicketDetailRepository = borrowTicketDetailRepository;
        this.borrowReminderEmailService = borrowReminderEmailService;
        this.notificationRepository = notificationRepository;
    }

    /**
     * Chạy tự động vào lúc 00:00:00 hàng đêm (hoặc theo cấu hình application.properties).
     * Cron mặc định: "0 0 0 * * ?" = mỗi ngày lúc 00:00:00.
     */
    @Scheduled(cron = "${app.scheduler.borrow-reminder.cron:0 0 0 * * ?}")
    public void scanAndNotify() {
        log.info("[BORROW REMINDER SCHEDULER] Bắt đầu quét sách sắp/đã quá hạn...");

        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();

        // Lấy toàn bộ sách đang mượn (chưa trả)
        List<BorrowTicketDetail> activeBorrows = borrowTicketDetailRepository.findCurrentlyBorrowing();
        log.info("[BORROW REMINDER SCHEDULER] Tổng số bản ghi đang mượn: {}", activeBorrows.size());

        // Gom nhóm danh sách nhắc nhở theo từng Patron
        Map<String, List<BorrowReminderInfo>> reminderMap = new LinkedHashMap<>();
        Map<String, User> patronMap = new LinkedHashMap<>();

        for (BorrowTicketDetail detail : activeBorrows) {
            if (detail.getDueDate() == null) continue;

            LocalDate dueDate = detail.getDueDate().toLocalDate();
            long daysUntilDue = ChronoUnit.DAYS.between(today, dueDate);
            long daysOverdue = ChronoUnit.DAYS.between(dueDate, today);

            BorrowReminderInfo info = null;

            if (daysUntilDue == daysBefore) {
                // Sắp hết hạn
                info = BorrowReminderInfo.builder()
                        .bookTitle(getBookTitle(detail))
                        .copyId(getCopyId(detail))
                        .dueDate(detail.getDueDate())
                        .statusLabel("Sắp hết hạn (còn " + daysBefore + " ngày)")
                        .overdue(false)
                        .build();
            } else if (daysOverdue > 0) {
                // Đã quá hạn
                info = BorrowReminderInfo.builder()
                        .bookTitle(getBookTitle(detail))
                        .copyId(getCopyId(detail))
                        .dueDate(detail.getDueDate())
                        .statusLabel("Đã quá hạn " + daysOverdue + " ngày")
                        .overdue(true)
                        .build();
            }

            if (info != null && detail.getBorrowTicket() != null && detail.getBorrowTicket().getPatron() != null) {
                User patron = detail.getBorrowTicket().getPatron();
                String patronId = patron.getUserId();
                reminderMap.computeIfAbsent(patronId, k -> new ArrayList<>()).add(info);
                patronMap.putIfAbsent(patronId, patron);
            }
        }

        log.info("[BORROW REMINDER SCHEDULER] Số bạn đọc cần nhắc nhở: {}", reminderMap.size());

        // Gửi email cho từng bạn đọc
        for (Map.Entry<String, List<BorrowReminderInfo>> entry : reminderMap.entrySet()) {
            String patronId = entry.getKey();
            User patron = patronMap.get(patronId);
            List<BorrowReminderInfo> reminders = entry.getValue();

            if (patron == null || patron.getEmail() == null || patron.getEmail().isBlank()) {
                log.warn("[BORROW REMINDER SCHEDULER] Bỏ qua patron {} - không có email", patronId);
                continue;
            }

            if (!isValidEmail(patron.getEmail())) {
                log.warn("[BORROW REMINDER SCHEDULER] Bỏ qua patron {} - email '{}' không đúng định dạng", patronId, patron.getEmail());
                continue;
            }

            try {
                borrowReminderEmailService.sendBorrowReminder(
                        patron.getEmail(),
                        patron.getFullName(),
                        reminders
                );
                saveBorrowNotification(patron, reminders, "Sent", null);
            } catch (Exception e) {
                log.error("[BORROW REMINDER SCHEDULER] Gửi email thất bại cho patron {} | Error: {}", patronId, e.getMessage());
                saveBorrowNotification(patron, reminders, "Failed", e.getMessage());
            }
        }

        log.info("[BORROW REMINDER SCHEDULER] Hoàn thành quét. Tổng email đã gửi: {}", reminderMap.size());
    }

    private void saveBorrowNotification(User patron, List<BorrowReminderInfo> reminders, String status, String errorMessage) {
        try {
            boolean hasOverdue = reminders.stream().anyMatch(BorrowReminderInfo::isOverdue);
            String title = hasOverdue
                    ? "[Thư viện FPT] ⚠️ Bạn đang có sách quá hạn trả!"
                    : "[Thư viện FPT] 🔔 Nhắc nhở: Sách sắp đến hạn trả";

            String htmlBody = borrowReminderEmailService.buildReminderBody(patron.getFullName(), reminders);

            if (errorMessage != null && !errorMessage.isBlank()) {
                String errorBlock = """
                        <div style="background:#ffebee; border-left:4px solid #f44336; padding:12px; border-radius:4px; margin: 16px auto; max-width:680px;">
                          <p style="margin:0; color:#c62828;">❌ <strong>Lỗi gửi email:</strong> %s</p>
                        </div>
                        """.formatted(errorMessage);
                int insertionPoint = htmlBody.indexOf("<div style=\"padding:24px;\">");
                if (insertionPoint != -1) {
                    int insertIndex = insertionPoint + "<div style=\"padding:24px;\">".length();
                    htmlBody = htmlBody.substring(0, insertIndex) + errorBlock + htmlBody.substring(insertIndex);
                } else {
                    htmlBody = errorBlock + htmlBody;
                }
            }

            Notification notification = Notification.builder()
                    .user(patron)
                    .notificationType("BORROW_REMINDER")
                    .title(title)
                    .content(htmlBody)
                    .status(status)
                    .sentAt(status.equals("Sent") ? LocalDateTime.now() : null)
                    .createdAt(LocalDateTime.now())
                    .read(false)
                    .build();

            notificationRepository.save(notification);
        } catch (Exception e) {
            log.error("Lỗi khi lưu Notification log cho bạn đọc {}: {}", patron.getUserId(), e.getMessage(), e);
        }
    }

    // ── Helper Methods ─────────────────────────────────────────────────────

    private String getBookTitle(BorrowTicketDetail detail) {
        try {
            return detail.getBookCopy().getBook().getTitle();
        } catch (Exception e) {
            return "Không xác định";
        }
    }

    private String getCopyId(BorrowTicketDetail detail) {
        try {
            return detail.getBookCopy().getCopyId();
        } catch (Exception e) {
            return "N/A";
        }
    }
}
