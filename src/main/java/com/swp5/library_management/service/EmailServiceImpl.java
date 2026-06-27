package com.swp5.library_management.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

/**
 * Triển khai EmailService dùng JavaMailSender (SMTP Gmail).
 * UCR06 bước 8: Tự động gửi email xác nhận khi đặt / hủy / waitlist.
 *
 * Cấu hình SMTP trong application.properties:
 *   spring.mail.username, spring.mail.password, app.mail.from
 */
@Service
public class EmailServiceImpl implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailServiceImpl.class);

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String fromAddress;

    public EmailServiceImpl(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    // ── Gửi xác nhận đặt giữ chỗ ──────────────────────────────────────────

    @Override
    public void sendReservationConfirmation(String toEmail, String patronName,
                                            String bookTitle, String campusName,
                                            String expiryInfo) {
        String subject = "[Thư viện FPT] Đặt giữ chỗ sách thành công";
        String body = buildReservationConfirmBody(patronName, bookTitle, campusName, expiryInfo);
        sendHtmlEmail(toEmail, subject, body);
    }

    // ── Gửi xác nhận hủy đặt chỗ ─────────────────────────────────────────

    @Override
    public void sendReservationCancellation(String toEmail, String patronName, String bookTitle) {
        String subject = "[Thư viện FPT] Hủy đặt giữ chỗ sách";
        String body = buildCancellationBody(patronName, bookTitle);
        sendHtmlEmail(toEmail, subject, body);
    }

    // ── Gửi xác nhận đăng ký waitlist ────────────────────────────────────

    @Override
    public void sendWaitlistConfirmation(String toEmail, String patronName,
                                         String bookTitle, long position) {
        String subject = "[Thư viện FPT] Đăng ký hàng đợi sách";
        String body = buildWaitlistBody(patronName, bookTitle, position);
        sendHtmlEmail(toEmail, subject, body);
    }

    // ── Gửi xác nhận hủy xếp hàng chờ ────────────────────────────────────

    @Override
    public void sendWaitlistCancellation(String toEmail, String patronName, String bookTitle) {
        String subject = "[Thư viện FPT] Hủy đăng ký xếp hàng chờ sách";
        String body = buildWaitlistCancellationBody(patronName, bookTitle);
        sendHtmlEmail(toEmail, subject, body);
    }

    // ── Gửi xác nhận tạo phiếu mượn ──────────────────────────────────────

    @Override
    public void sendLoanConfirmation(String toEmail, String patronName, String bookTitle, String copyId, String dueDate) {
        String subject = "[Thư viện FPT] Đăng ký mượn sách thành công";
        String body = buildLoanConfirmationBody(patronName, bookTitle, copyId, dueDate);
        sendHtmlEmail(toEmail, subject, body);
    }

    // ── Gửi xác nhận đề nghị tài liệu mới ─────────────────────────────────

    @Override
    public void sendMaterialRequestConfirmation(String toEmail, String patronName, String bookTitle, String author, String priority) {
        String subject = "[Thư viện FPT] Đăng ký đề nghị tài liệu mới thành công";
        String body = buildMaterialRequestConfirmBody(patronName, bookTitle, author, priority);
        sendHtmlEmail(toEmail, subject, body);
    }

    @Override
    public void sendMaterialRequestApproval(String toEmail, String patronName, String bookTitle, String author) {
        String subject = "[Thư viện FPT] Yêu cầu đề nghị tài liệu mới đã được duyệt";
        String body = buildMaterialRequestApprovalBody(patronName, bookTitle, author);
        sendHtmlEmail(toEmail, subject, body);
    }

    // ── Helper: Gửi email HTML ────────────────────────────────────────────

    private void sendHtmlEmail(String to, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true); // true = HTML
            mailSender.send(message);
            log.info("[EMAIL SENT] To: {} | Subject: {}", to, subject);
        } catch (Exception e) {
            // Lỗi gửi mail (MessagingException hoặc Spring MailException) không nên làm gián đoạn luồng nghiệp vụ chính
            log.error("[EMAIL FAILED] To: {} | Subject: {} | Error: {}", to, subject, e.getMessage());
        }
    }

    // ── Template HTML email ───────────────────────────────────────────────

    private String buildReservationConfirmBody(String patronName, String bookTitle,
                                               String campusName, String expiryInfo) {
        return """
                <html><body style="font-family: Arial, sans-serif; color: #333;">
                  <div style="max-width:600px; margin:auto; border:1px solid #e0e0e0; border-radius:8px; overflow:hidden;">
                    <div style="background:#003580; padding:20px; text-align:center;">
                      <h2 style="color:#fff; margin:0;">📚 Thư viện FPT University</h2>
                    </div>
                    <div style="padding:24px;">
                      <p>Xin chào <strong>%s</strong>,</p>
                      <p>Đơn đặt giữ chỗ sách của bạn đã được xác nhận thành công!</p>
                      <table style="width:100%%; border-collapse:collapse; margin:16px 0;">
                        <tr style="background:#f5f5f5;">
                          <td style="padding:10px; border:1px solid #ddd;"><strong>📖 Tên sách</strong></td>
                          <td style="padding:10px; border:1px solid #ddd;">%s</td>
                        </tr>
                        <tr>
                          <td style="padding:10px; border:1px solid #ddd;"><strong>🏫 Cơ sở nhận sách</strong></td>
                          <td style="padding:10px; border:1px solid #ddd;">%s</td>
                        </tr>
                        <tr style="background:#f5f5f5;">
                          <td style="padding:10px; border:1px solid #ddd;"><strong>⏰ Thời hạn đến nhận</strong></td>
                          <td style="padding:10px; border:1px solid #ddd; color:#e53935;"><strong>%s</strong></td>
                        </tr>
                      </table>
                      <div style="background:#fff3e0; border-left:4px solid #ff9800; padding:12px; border-radius:4px;">
                        <p style="margin:0;">⚠️ <strong>Lưu ý:</strong> Nếu bạn không đến nhận sách trong thời hạn trên,
                        đơn đặt chỗ sẽ tự động hủy và bản sách sẽ được phục vụ cho bạn đọc khác.</p>
                      </div>
                      <p style="margin-top:20px;">Trân trọng,<br><strong>Hệ thống Thư viện FPT University</strong></p>
                    </div>
                  </div>
                </body></html>
                """.formatted(patronName, bookTitle, campusName, expiryInfo);
    }

    private String buildCancellationBody(String patronName, String bookTitle) {
        return """
                <html><body style="font-family: Arial, sans-serif; color: #333;">
                  <div style="max-width:600px; margin:auto; border:1px solid #e0e0e0; border-radius:8px; overflow:hidden;">
                    <div style="background:#003580; padding:20px; text-align:center;">
                      <h2 style="color:#fff; margin:0;">📚 Thư viện FPT University</h2>
                    </div>
                    <div style="padding:24px;">
                      <p>Xin chào <strong>%s</strong>,</p>
                      <p>Đơn đặt giữ chỗ sách của bạn đã được hủy thành công.</p>
                      <table style="width:100%%; border-collapse:collapse; margin:16px 0;">
                        <tr style="background:#f5f5f5;">
                          <td style="padding:10px; border:1px solid #ddd;"><strong>📖 Tên sách đã hủy</strong></td>
                          <td style="padding:10px; border:1px solid #ddd;">%s</td>
                        </tr>
                      </table>
                      <p>Bản sách đã được trả về kho và sẵn sàng cho bạn đọc khác.</p>
                      <p>Nếu bạn vẫn muốn mượn sách này, hãy thực hiện đặt giữ chỗ mới trên hệ thống.</p>
                      <p style="margin-top:20px;">Trân trọng,<br><strong>Hệ thống Thư viện FPT University</strong></p>
                    </div>
                  </div>
                </body></html>
                """.formatted(patronName, bookTitle);
    }

    private String buildWaitlistBody(String patronName, String bookTitle, long position) {
        return """
                <html><body style="font-family: Arial, sans-serif; color: #333;">
                  <div style="max-width:600px; margin:auto; border:1px solid #e0e0e0; border-radius:8px; overflow:hidden;">
                    <div style="background:#003580; padding:20px; text-align:center;">
                      <h2 style="color:#fff; margin:0;">📚 Thư viện FPT University</h2>
                    </div>
                    <div style="padding:24px;">
                      <p>Xin chào <strong>%s</strong>,</p>
                      <p>Bạn đã được đăng ký vào danh sách chờ cho cuốn sách:</p>
                      <table style="width:100%%; border-collapse:collapse; margin:16px 0;">
                        <tr style="background:#f5f5f5;">
                          <td style="padding:10px; border:1px solid #ddd;"><strong>📖 Tên sách</strong></td>
                          <td style="padding:10px; border:1px solid #ddd;">%s</td>
                        </tr>
                        <tr>
                          <td style="padding:10px; border:1px solid #ddd;"><strong>🔢 Số thứ tự của bạn</strong></td>
                          <td style="padding:10px; border:1px solid #ddd; font-size:1.4em; color:#003580;"><strong>#%d</strong></td>
                        </tr>
                      </table>
                      <div style="background:#e8f5e9; border-left:4px solid #4caf50; padding:12px; border-radius:4px;">
                        <p style="margin:0;">✅ Hệ thống sẽ tự động thông báo cho bạn qua email ngay khi có bản sách sẵn sàng.</p>
                      </div>
                      <p style="margin-top:20px;">Trân trọng,<br><strong>Hệ thống Thư viện FPT University</strong></p>
                    </div>
                  </div>
                </body></html>
                """.formatted(patronName, bookTitle, position);
    }

    private String buildWaitlistCancellationBody(String patronName, String bookTitle) {
        return """
                <html><body style="font-family: Arial, sans-serif; color: #333;">
                  <div style="max-width:600px; margin:auto; border:1px solid #e0e0e0; border-radius:8px; overflow:hidden;">
                    <div style="background:#003580; padding:20px; text-align:center;">
                      <h2 style="color:#fff; margin:0;">📚 Thư viện FPT University</h2>
                    </div>
                    <div style="padding:24px;">
                      <p>Xin chào <strong>%s</strong>,</p>
                      <p>Bạn đã hủy đăng ký xếp hàng chờ thành công cho cuốn sách:</p>
                      <table style="width:100%%; border-collapse:collapse; margin:16px 0;">
                        <tr style="background:#f5f5f5;">
                          <td style="padding:10px; border:1px solid #ddd;"><strong>📖 Tên sách đã hủy chờ</strong></td>
                          <td style="padding:10px; border:1px solid #ddd;">%s</td>
                        </tr>
                      </table>
                      <p>Hệ thống đã rút tên bạn ra khỏi danh sách xếp hàng chờ cho cuốn sách này.</p>
                      <p>Nếu bạn có nhu cầu xếp hàng chờ lại, hãy đăng ký mới trên hệ thống.</p>
                      <p style="margin-top:20px;">Trân trọng,<br><strong>Hệ thống Thư viện FPT University</strong></p>
                    </div>
                  </div>
                </body></html>
                """.formatted(patronName, bookTitle);
    }

    private String buildLoanConfirmationBody(String patronName, String bookTitle, String copyId, String dueDate) {
        return """
                <html><body style="font-family: Arial, sans-serif; color: #333;">
                  <div style="max-width:600px; margin:auto; border:1px solid #e0e0e0; border-radius:8px; overflow:hidden;">
                    <div style="background:#003580; padding:20px; text-align:center;">
                      <h2 style="color:#fff; margin:0;">📚 Thư viện FPT University</h2>
                    </div>
                    <div style="padding:24px;">
                      <p>Xin chào <strong>%s</strong>,</p>
                      <p>Phiếu mượn sách của bạn đã được lập thành công tại quầy thủ thư.</p>
                      <table style="width:100%%; border-collapse:collapse; margin:16px 0;">
                        <tr style="background:#f5f5f5;">
                          <td style="padding:10px; border:1px solid #ddd;"><strong>📖 Tên sách</strong></td>
                          <td style="padding:10px; border:1px solid #ddd;">%s</td>
                        </tr>
                        <tr>
                          <td style="padding:10px; border:1px solid #ddd;"><strong>🔖 Mã bản sao</strong></td>
                          <td style="padding:10px; border:1px solid #ddd;">%s</td>
                        </tr>
                        <tr style="background:#f5f5f5;">
                          <td style="padding:10px; border:1px solid #ddd;"><strong>⏰ Hạn trả sách</strong></td>
                          <td style="padding:10px; border:1px solid #ddd; color:#e53935;"><strong>%s</strong></td>
                        </tr>
                      </table>
                      <div style="background:#fff3e0; border-left:4px solid #ff9800; padding:12px; border-radius:4px;">
                        <p style="margin:0;">⚠️ <strong>Lưu ý:</strong> Vui lòng hoàn trả sách trước hạn để tránh phát sinh phí phạt quá hạn nhé!</p>
                      </div>
                      <p style="margin-top:20px;">Chúc bạn đọc sách vui vẻ!<br><strong>Hệ thống Thư viện FPT University</strong></p>
                    </div>
                  </div>
                </body></html>
                """.formatted(patronName, bookTitle, copyId, dueDate);
    }

    private String buildMaterialRequestConfirmBody(String patronName, String bookTitle, String author, String priority) {
        return """
                <html><body style="font-family: Arial, sans-serif; color: #333;">
                  <div style="max-width:600px; margin:auto; border:1px solid #e0e0e0; border-radius:8px; overflow:hidden;">
                    <div style="background:#e87722; padding:20px; text-align:center;">
                      <h2 style="color:#fff; margin:0;">📚 Thư viện FPT University</h2>
                    </div>
                    <div style="padding:24px;">
                      <p>Xin chào <strong>%s</strong>,</p>
                      <p>Yêu cầu đề nghị tài liệu mới của bạn đã được tiếp nhận thành công!</p>
                      <table style="width:100%%; border-collapse:collapse; margin:16px 0;">
                        <tr style="background:#f5f5f5;">
                          <td style="padding:10px; border:1px solid #ddd;"><strong>📖 Tên tài liệu</strong></td>
                          <td style="padding:10px; border:1px solid #ddd;">%s</td>
                        </tr>
                        <tr>
                          <td style="padding:10px; border:1px solid #ddd;"><strong>✍️ Tác giả</strong></td>
                          <td style="padding:10px; border:1px solid #ddd;">%s</td>
                        </tr>
                        <tr style="background:#f5f5f5;">
                          <td style="padding:10px; border:1px solid #ddd;"><strong>⚡ Mức độ ưu tiên</strong></td>
                          <td style="padding:10px; border:1px solid #ddd;"><strong>%s</strong></td>
                        </tr>
                      </table>
                      <div style="background:#e8f5e9; border-left:4px solid #4caf50; padding:12px; border-radius:4px;">
                        <p style="margin:0;">✅ Đơn đề nghị của bạn đang ở trạng thái <strong>Chờ duyệt</strong>. Thủ thư sẽ xem xét và phản hồi trong thời gian sớm nhất.</p>
                      </div>
                      <p style="margin-top:20px;">Trân trọng,<br><strong>Hệ thống Thư viện FPT University</strong></p>
                    </div>
                  </div>
                </body></html>
                """.formatted(patronName, bookTitle, author, priority);
    }

    private String buildMaterialRequestApprovalBody(String patronName, String bookTitle, String author) {
        return """
                <html><body style="font-family: Arial, sans-serif; color: #333;">
                  <div style="max-width:600px; margin:auto; border:1px solid #e0e0e0; border-radius:8px; overflow:hidden;">
                    <div style="background:#4caf50; padding:20px; text-align:center;">
                      <h2 style="color:#fff; margin:0;">📚 Thư viện FPT University</h2>
                    </div>
                    <div style="padding:24px;">
                      <p>Xin chào <strong>%s</strong>,</p>
                      <p>Yêu cầu đề nghị tài liệu mới của bạn đã được <strong>DUYỆT</strong>!</p>
                      <table style="width:100%%; border-collapse:collapse; margin:16px 0;">
                        <tr style="background:#f5f5f5;">
                          <td style="padding:10px; border:1px solid #ddd;"><strong>📖 Tên tài liệu</strong></td>
                          <td style="padding:10px; border:1px solid #ddd;">%s</td>
                        </tr>
                        <tr>
                          <td style="padding:10px; border:1px solid #ddd;"><strong>✍️ Tác giả</strong></td>
                          <td style="padding:10px; border:1px solid #ddd;">%s</td>
                        </tr>
                      </table>
                      <div style="background:#e8f5e9; border-left:4px solid #4caf50; padding:12px; border-radius:4px;">
                        <p style="margin:0;">✅ Thư viện đang tiến hành bổ sung tài liệu này. Cảm ơn sự đóng góp của bạn!</p>
                      </div>
                      <p style="margin-top:20px;">Trân trọng,<br><strong>Hệ thống Thư viện FPT University</strong></p>
                    </div>
                  </div>
                </body></html>
                """.formatted(patronName, bookTitle, author);
    }
}
