package com.swp5.library_management.service;

import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * Service gửi email riêng cho tính năng kiểm tra nghĩa vụ tốt nghiệp.
 * Tách riêng khỏi EmailService/EmailServiceImpl chung.
 */
@Service
@Slf4j
public class GraduationEmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String fromAddress;

    public GraduationEmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Gửi email thông báo vi phạm nghĩa vụ thư viện.
     *
     * @param toEmail       Địa chỉ email sinh viên
     * @param patronName    Tên sinh viên
     * @param studentId     Mã số sinh viên
     * @param violationRows Các dòng HTML chi tiết vi phạm (copyId, tên sách, lý do)
     */
    public void sendViolationNotification(String toEmail, String patronName,
                                           String studentId, String violationRows) {
        String subject = "[Thư viện FPT] Thông báo nghĩa vụ thư viện chưa hoàn thành";
        String body = buildViolationBody(patronName, studentId, violationRows);

        log.info("[GRADUATION EMAIL START] Preparing mail to: {} | Student: {}", toEmail, studentId);
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(body, true);
            log.info("[GRADUATION EMAIL SENDING] Invoking mailSender.send to: {}", toEmail);
            mailSender.send(message);
            log.info("[GRADUATION EMAIL SENT] To: {} | Student: {}", toEmail, studentId);
        } catch (Exception e) {
            log.error("[GRADUATION EMAIL FAILED] To: {} | Student: {} | Error: {}", toEmail, studentId, e.getMessage());
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private String buildViolationBody(String patronName, String studentId, String violationRows) {
        return """
                <html><body style="font-family: Arial, sans-serif; color: #333;">
                  <div style="max-width:650px; margin:auto; border:1px solid #e0e0e0; border-radius:8px; overflow:hidden;">
                    <div style="background:#c62828; padding:20px; text-align:center;">
                      <h2 style="color:#fff; margin:0;">⚠️ Thông báo nghĩa vụ thư viện</h2>
                    </div>
                    <div style="padding:24px;">
                      <p>Xin chào <strong>%s</strong> (MSSV: <strong>%s</strong>),</p>
                      <p>Hệ thống phát hiện bạn <strong style="color:#c62828;">chưa hoàn thành</strong> nghĩa vụ thư viện. Chi tiết vi phạm như sau:</p>
                      <table style="width:100%%; border-collapse:collapse; margin:16px 0;">
                        <tr style="background:#f5f5f5;">
                          <th style="padding:10px; border:1px solid #ddd; text-align:left;">Mã bản sao</th>
                          <th style="padding:10px; border:1px solid #ddd; text-align:left;">Tên sách</th>
                          <th style="padding:10px; border:1px solid #ddd; text-align:left;">Lý do</th>
                        </tr>
                        %s
                      </table>
                      <div style="background:#fff3e0; border-left:4px solid #ff9800; padding:12px; border-radius:4px;">
                        <p style="margin:0;">⚠️ <strong>Lưu ý:</strong> Bạn cần hoàn thành các nghĩa vụ trên trước khi có thể xác nhận đủ điều kiện tốt nghiệp. Vui lòng liên hệ thư viện để giải quyết sớm nhất.</p>
                      </div>
                      <p style="margin-top:20px;">Trân trọng,<br><strong>Hệ thống Thư viện FPT University</strong></p>
                    </div>
                  </div>
                </body></html>
                """.formatted(patronName, studentId, violationRows);
    }
}
