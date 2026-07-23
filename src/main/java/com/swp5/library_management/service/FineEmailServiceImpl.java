package com.swp5.library_management.service;

import com.swp5.library_management.entity.FineInvoice;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class FineEmailServiceImpl implements FineEmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String fromAddress;

    @Override
    public void sendFinePaymentConfirmation(FineInvoice fine) {
        if (fine == null || fine.getPatron() == null || fine.getPatron().getEmail() == null) {
            log.warn("[FINE EMAIL] Skipping - no patron email for invoice ID: {}", fine != null ? fine.getFineId() : "null");
            return;
        }

        String toEmail = fine.getPatron().getEmail();
        String subject = "[Thư viện FPT] Xác nhận thanh toán tiền phạt";
        String body = buildFinePaymentBody(fine);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(body, true);
            mailSender.send(message);
            log.info("[FINE EMAIL SENT] To: {} | Invoice: {}", toEmail, fine.getFineId());
        } catch (Exception e) {
            log.error("[FINE EMAIL FAILED] To: {} | Error: {}", toEmail, e.getMessage());
        }
    }

    private String buildFinePaymentBody(FineInvoice fine) {
        String patronName = fine.getPatron().getFullName();
        String bookTitle = fine.getTicketDetail() != null && fine.getTicketDetail().getBookCopy() != null 
                           ? fine.getTicketDetail().getBookCopy().getBook().getTitle() : "N/A";
        String fineAmount = String.format("%,.0f VND", fine.getFineAmount());
        String paidDate = fine.getPaidAt() != null 
                          ? fine.getPaidAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) 
                          : "N/A";
        String violationType = fine.getViolationType();
        String reason = fine.getReason();
        String rawMethod = fine.getPaymentMethod();
        String paymentMethodDisplay = "Cash".equalsIgnoreCase(rawMethod) || rawMethod == null ? "Tiền mặt" : rawMethod;

        return """
                <html><body style="font-family: Arial, sans-serif; color: #333;">
                  <div style="max-width:600px; margin:auto; border:1px solid #e0e0e0; border-radius:8px; overflow:hidden;">
                    <div style="background:#F27125; padding:20px; text-align:center;">
                      <h2 style="color:#fff; margin:0;">📚 Thư viện FPT University</h2>
                    </div>
                    <div style="padding:24px;">
                      <p>Xin chào <strong>%s</strong>,</p>
                      <p>Hệ thống đã ghi nhận thanh toán tiền phạt của bạn thành công.</p>
                      <table style="width:100%%; border-collapse:collapse; margin:16px 0;">
                        <tr style="background:#f5f5f5;">
                          <td style="padding:10px; border:1px solid #ddd;"><strong>📖 Tên sách</strong></td>
                          <td style="padding:10px; border:1px solid #ddd;">%s</td>
                        </tr>
                        <tr>
                          <td style="padding:10px; border:1px solid #ddd;"><strong>⚠️ Loại vi phạm</strong></td>
                          <td style="padding:10px; border:1px solid #ddd;">%s</td>
                        </tr>
                        <tr style="background:#f5f5f5;">
                          <td style="padding:10px; border:1px solid #ddd;"><strong>💬 Lý do</strong></td>
                          <td style="padding:10px; border:1px solid #ddd;">%s</td>
                        </tr>
                        <tr>
                          <td style="padding:10px; border:1px solid #ddd;"><strong>💳 Phương thức thanh toán</strong></td>
                          <td style="padding:10px; border:1px solid #ddd;"><strong>%s</strong></td>
                        </tr>
                        <tr style="background:#f5f5f5;">
                          <td style="padding:10px; border:1px solid #ddd;"><strong>💰 Số tiền đã thanh toán</strong></td>
                          <td style="padding:10px; border:1px solid #ddd; color:#2e7d32;"><strong>%s</strong></td>
                        </tr>
                        <tr>
                          <td style="padding:10px; border:1px solid #ddd;"><strong>📅 Thời gian thanh toán</strong></td>
                          <td style="padding:10px; border:1px solid #ddd;">%s</td>
                        </tr>
                      </table>
                      <div style="background:#e8f5e9; border-left:4px solid #4caf50; padding:12px; border-radius:4px;">
                        <p style="margin:0;">✅ Cảm ơn bạn đã hoàn thành nghĩa vụ thanh toán. Bạn có thể tiếp tục sử dụng các dịch vụ của thư viện.</p>
                      </div>
                      <p style="margin-top:20px;">Trân trọng,<br><strong>Hệ thống Thư viện FPT University</strong></p>
                    </div>
                  </div>
                </body></html>
                """.formatted(patronName, bookTitle, violationType, reason, paymentMethodDisplay, fineAmount, paidDate);
    }
}
