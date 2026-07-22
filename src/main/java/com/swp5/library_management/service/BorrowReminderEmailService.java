package com.swp5.library_management.service;

import com.swp5.library_management.dto.BorrowReminderInfo;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Service gửi email nhắc nhở hạn trả sách.
 * Tách riêng khỏi EmailService/EmailServiceImpl để tránh xung đột code với thành viên khác.
 * Theo pattern của GraduationEmailService.
 */
@Service
@Slf4j
public class BorrowReminderEmailService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String fromAddress;

    public BorrowReminderEmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Gửi email tổng hợp nhắc nhở danh sách sách sắp/đã quá hạn cho một bạn đọc.
     *
     * @param toEmail    Địa chỉ email bạn đọc
     * @param patronName Tên bạn đọc
     * @param reminders  Danh sách sách cần nhắc nhở
     */
    public void sendBorrowReminder(String toEmail, String patronName, List<BorrowReminderInfo> reminders) throws Exception {
        boolean hasOverdue = reminders.stream().anyMatch(BorrowReminderInfo::isOverdue);
        String subject = hasOverdue
                ? "[Thư viện FPT] ⚠️ Bạn đang có sách quá hạn trả!"
                : "[Thư viện FPT] 🔔 Nhắc nhở: Sách sắp đến hạn trả";

        String body = buildReminderBody(patronName, reminders);

        log.info("[BORROW REMINDER] Gửi email đến: {} | Patron: {} | Số sách cần nhắc: {}",
                toEmail, patronName, reminders.size());
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(fromAddress);
        helper.setTo(toEmail);
        helper.setSubject(subject);
        helper.setText(body, true);
        mailSender.send(message);
        log.info("[BORROW REMINDER SENT] To: {} | Patron: {}", toEmail, patronName);
    }

    // ── Template Builder ───────────────────────────────────────────────────

    public String buildReminderBody(String patronName, List<BorrowReminderInfo> reminders) {
        StringBuilder rows = new StringBuilder();
        for (BorrowReminderInfo r : reminders) {
            String rowStyle = r.isOverdue()
                    ? "background:#fdecea;"
                    : "background:#fff8e1;";
            String badgeStyle = r.isOverdue()
                    ? "background:#c62828; color:#fff; padding:3px 10px; border-radius:12px; font-size:0.85em;"
                    : "background:#f57f17; color:#fff; padding:3px 10px; border-radius:12px; font-size:0.85em;";
            String dueDateStr = r.getDueDate() != null ? r.getDueDate().format(DATE_FMT) : "N/A";

            rows.append("""
                    <tr style="%s">
                      <td style="padding:10px; border:1px solid #ddd;">%s</td>
                      <td style="padding:10px; border:1px solid #ddd; font-family:monospace;">%s</td>
                      <td style="padding:10px; border:1px solid #ddd; color:#c62828; font-weight:bold;">%s</td>
                      <td style="padding:10px; border:1px solid #ddd; text-align:center;">
                        <span style="%s">%s</span>
                      </td>
                    </tr>
                    """.formatted(rowStyle,
                    r.getBookTitle(),
                    r.getCopyId(),
                    dueDateStr,
                    badgeStyle,
                    r.getStatusLabel()));
        }

        return """
                <html><body style="font-family: Arial, sans-serif; color: #333; margin:0; padding:0;">
                  <div style="max-width:680px; margin:auto; border:1px solid #e0e0e0; border-radius:8px; overflow:hidden;">
                    <div style="background:#003580; padding:20px; text-align:center;">
                      <h2 style="color:#fff; margin:0;">📚 Thư viện FPT University</h2>
                      <p style="color:#cfe2ff; margin:4px 0 0;">Thông báo hạn trả sách</p>
                    </div>
                    <div style="padding:24px;">
                      <p>Xin chào <strong>%s</strong>,</p>
                      <p>Hệ thống phát hiện bạn có sách cần chú ý về thời hạn trả. Vui lòng kiểm tra danh sách bên dưới:</p>
                      <table style="width:100%%; border-collapse:collapse; margin:16px 0;">
                        <tr style="background:#003580; color:#fff;">
                          <th style="padding:10px; border:1px solid #ddd; text-align:left;">📖 Tên sách</th>
                          <th style="padding:10px; border:1px solid #ddd; text-align:left;">🔖 Mã bản sao</th>
                          <th style="padding:10px; border:1px solid #ddd; text-align:left;">⏰ Hạn trả</th>
                          <th style="padding:10px; border:1px solid #ddd; text-align:center;">Trạng thái</th>
                        </tr>
                        %s
                      </table>
                      <div style="background:#e3f2fd; border-left:4px solid #003580; padding:12px; border-radius:4px; margin-top:16px;">
                        <p style="margin:0;">💡 <strong>Lưu ý:</strong> Vui lòng đến quầy thủ thư hoặc liên hệ thư viện để gia hạn hoặc hoàn trả sách kịp thời, tránh phát sinh vi phạm.</p>
                      </div>
                      <p style="margin-top:20px;">Trân trọng,<br><strong>Hệ thống Thư viện FPT University</strong></p>
                    </div>
                  </div>
                </body></html>
                """.formatted(patronName, rows.toString());
    }
}
