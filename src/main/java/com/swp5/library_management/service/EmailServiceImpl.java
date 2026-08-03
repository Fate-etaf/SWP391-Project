package com.swp5.library_management.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

/**
 * Triển khai EmailService dùng JavaMailSender (SMTP Gmail).
 * UCR06 bước 8: Tự động gửi email xác nhận khi đặt / hủy / waitlist.
 *
 * Cấu hình SMTP trong application.properties:
 * spring.mail.username, spring.mail.password, app.mail.from
 */
@Service
@Async
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
  public void sendMaterialRequestConfirmation(String toEmail, String patronName, String bookTitle, String author,
      String priority) {
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

  @Override
  public void sendMaterialRequestRejection(String toEmail, String patronName, String bookTitle, String author) {
    String subject = "[Thư viện FPT] Yêu cầu đề nghị tài liệu mới không được duyệt";
    String body = buildMaterialRequestRejectionBody(patronName, bookTitle, author);
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
      // Lỗi gửi mail (MessagingException hoặc Spring MailException) không nên làm
      // gián đoạn luồng nghiệp vụ chính
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
        """
        .formatted(patronName, bookTitle, position);
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
        """
        .formatted(patronName, bookTitle, copyId, dueDate);
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
        """
        .formatted(patronName, bookTitle, author, priority);
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
              <p>Chúng tôi vui mừng thông báo rằng <strong>đề nghị tài liệu mới</strong> của bạn đã được <strong>phê duyệt</strong>.</p>

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
                  <td style="padding:10px; border:1px solid #ddd;"><strong>📌 Trạng thái</strong></td>
                  <td style="padding:10px; border:1px solid #ddd;">
                    <strong style="color:#4caf50;">Đã phê duyệt</strong>
                  </td>
                </tr>
              </table>

              <div style="background:#e8f5e9; border-left:4px solid #4caf50; padding:12px; border-radius:4px;">
                <p style="margin:0;">
                  🎉 Cảm ơn bạn đã gửi đề nghị. Thư viện sẽ tiến hành các bước tiếp theo để bổ sung tài liệu vào hệ thống trong thời gian sớm nhất.
                </p>
              </div>

              <p style="margin-top:20px;">
                Trân trọng,<br>
                <strong>Hệ thống Thư viện FPT University</strong>
              </p>
            </div>
          </div>
        </body></html>
        """
        .formatted(patronName, bookTitle, author);
  }

  private String buildMaterialRequestRejectionBody(String patronName, String bookTitle, String author) {
    return """
        <html><body style="font-family: Arial, sans-serif; color: #333;">
          <div style="max-width:600px; margin:auto; border:1px solid #e0e0e0; border-radius:8px; overflow:hidden;">
            <div style="background:#f44336; padding:20px; text-align:center;">
              <h2 style="color:#fff; margin:0;">📚 Thư viện FPT University</h2>
            </div>
            <div style="padding:24px;">
              <p>Xin chào <strong>%s</strong>,</p>
              <p>Chúng tôi rất tiếc phải thông báo rằng <strong>đề nghị tài liệu mới</strong> của bạn đã <strong>không được phê duyệt</strong>.</p>

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
                  <td style="padding:10px; border:1px solid #ddd;"><strong>📌 Trạng thái</strong></td>
                  <td style="padding:10px; border:1px solid #ddd;">
                    <strong style="color:#f44336;">Không được phê duyệt</strong>
                  </td>
                </tr>
              </table>

              <div style="background:#ffebee; border-left:4px solid #f44336; padding:12px; border-radius:4px;">
                <p style="margin:0;">
                  Thư viện chưa thể bổ sung tài liệu này vào thời điểm hiện tại do giới hạn ngân sách hoặc chính sách phát triển bộ sưu tập. Mong bạn thông cảm.
                </p>
              </div>

              <p style="margin-top:20px;">
                Trân trọng,<br>
                <strong>Hệ thống Thư viện FPT University</strong>
              </p>
            </div>
          </div>
        </body></html>
        """
        .formatted(patronName, bookTitle, author);
  }

  @Override
  public void sendOtpEmail(String toEmail, String subject, String content) {
    try {
      // 1. Tạo cấu trúc tin nhắn Mail đơn giản của Spring
      org.springframework.mail.SimpleMailMessage message = new org.springframework.mail.SimpleMailMessage();
      message.setTo(toEmail); // Người nhận (Email sinh viên)
      message.setSubject(subject); // Tiêu đề email
      message.setText(content); // Nội dung chứa mã OTP

      // 2. Ra lệnh cho bộ gửi mail bắn tin nhắn đi sang Server Google
      mailSender.send(message);
      System.out.println("======> ĐÃ GỬI MAIL THÀNH CÔNG TỚI GMAIL THẬT: " + toEmail);
    } catch (Exception e) {
      System.out.println("======> LỖI KẾT NỐI SMTP GMAIL: " + e.getMessage());
      throw new RuntimeException(e);
    }
  }

  @Override
  public void sendReturnConfirmation(String toEmail, String patronName, String bookTitle, String copyId,
      String returnDate, String fineAmount, String paymentMethod, String status) {
    String subject = "[Thư viện FPT] Xác nhận trả sách thành công";
    String body = buildReturnConfirmationBody(patronName, bookTitle, copyId, returnDate, fineAmount, paymentMethod,
        status);
    sendHtmlEmail(toEmail, subject, body);
  }

  private String buildReturnConfirmationBody(String patronName, String bookTitle, String copyId,
      String returnDate, String fineAmount, String paymentMethod, String status) {
    return """
        <html><body style="font-family: Arial, sans-serif; color: #333;">
          <div style="max-width:600px; margin:auto; border:1px solid #e0e0e0; border-radius:8px; overflow:hidden;">
            <div style="background:#4caf50; padding:20px; text-align:center;">
              <h2 style="color:#fff; margin:0;">📚 Thư viện FPT University</h2>
            </div>
            <div style="padding:24px;">
              <p>Xin chào <strong>%s</strong>,</p>
              <p>Hệ thống đã xác nhận bạn hoàn trả sách thành công.</p>
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
                  <td style="padding:10px; border:1px solid #ddd;"><strong>📅 Ngày trả sách</strong></td>
                  <td style="padding:10px; border:1px solid #ddd;">%s</td>
                </tr>
                <tr>
                  <td style="padding:10px; border:1px solid #ddd;"><strong>📌 Trạng thái trả</strong></td>
                  <td style="padding:10px; border:1px solid #ddd;"><strong>%s</strong></td>
                </tr>
                <tr style="background:#f5f5f5;">
                  <td style="padding:10px; border:1px solid #ddd;"><strong>💰 Khoản phạt vi phạm</strong></td>
                  <td style="padding:10px; border:1px solid #ddd;">%s</td>
                </tr>
                <tr>
                  <td style="padding:10px; border:1px solid #ddd;"><strong>💳 Phương thức thanh toán</strong></td>
                  <td style="padding:10px; border:1px solid #ddd;">%s</td>
                </tr>
              </table>
              <div style="background:#e8f5e9; border-left:4px solid #4caf50; padding:12px; border-radius:4px;">
                <p style="margin:0;">✅ Thông tin giao dịch trả sách của bạn đã được đối lưu và cập nhật trên hệ thống quản lý thư viện.</p>
              </div>
              <p style="margin-top:20px;">Trân trọng,<br><strong>Hệ thống Thư viện FPT University</strong></p>
            </div>
          </div>
        </body></html>
        """
        .formatted(patronName, bookTitle, copyId, returnDate, status, fineAmount, paymentMethod);
  }

  // ==========================================
  // STUDY ROOM NOTIFICATIONS
  // ==========================================

  @Override
  public void sendStudyRoomBookingConfirmation(String toEmail, String patronName, String roomName, String date,
      String timeSlot) {
    String subject = "[Thư viện FPT] Xác nhận đặt phòng học nhóm thành công";
    String body = buildStudyRoomBookingConfirmBody(patronName, roomName, date, timeSlot);
    sendHtmlEmail(toEmail, subject, body);
  }

  @Override
  public void sendStudyRoomNoShow(String toEmail, String patronName, String roomName, String date, String timeSlot) {
    String subject = "[Thư viện FPT] Hủy phòng học nhóm do không Check-in (No Show)";
    String body = buildStudyRoomNoShowBody(patronName, roomName, date, timeSlot);
    sendHtmlEmail(toEmail, subject, body);
  }

  @Override
  public void sendStudyRoomCancellation(String toEmail, String patronName, String roomName, String date,
      String timeSlot) {
    String subject = "[Thư viện FPT] Hủy đặt phòng học nhóm";
    String body = buildStudyRoomCancellationBody(patronName, roomName, date, timeSlot);
    sendHtmlEmail(toEmail, subject, body);
  }

  private String buildStudyRoomBookingConfirmBody(String patronName, String roomName, String date, String timeSlot) {
    return """
        <html><body style="font-family: Arial, sans-serif; color: #333;">
          <div style="max-width:600px; margin:auto; border:1px solid #e0e0e0; border-radius:8px; overflow:hidden;">
            <div style="background:#003580; padding:20px; text-align:center;">
              <h2 style="color:#fff; margin:0;">📚 Thư viện FPT University</h2>
            </div>
            <div style="padding:24px;">
              <p>Xin chào <strong>%s</strong>,</p>
              <p>Đơn đặt phòng học nhóm của bạn đã được xác nhận thành công!</p>
              <table style="width:100%%; border-collapse:collapse; margin:16px 0;">
                <tr style="background:#f5f5f5;">
                   <td style="padding:10px; border:1px solid #ddd;"><strong>🏠 Phòng</strong></td>
                   <td style="padding:10px; border:1px solid #ddd;">%s</td>
                </tr>
                <tr>
                   <td style="padding:10px; border:1px solid #ddd;"><strong>📅 Ngày</strong></td>
                   <td style="padding:10px; border:1px solid #ddd;">%s</td>
                </tr>
                <tr style="background:#f5f5f5;">
                   <td style="padding:10px; border:1px solid #ddd;"><strong>⏰ Khung giờ</strong></td>
                   <td style="padding:10px; border:1px solid #ddd; color:#e53935;"><strong>%s</strong></td>
                </tr>
              </table>
              <div style="background:#fff3e0; border-left:4px solid #ff9800; padding:12px; border-radius:4px;">
                <p style="margin:0;">⚠️ <strong>Lưu ý:</strong> Vui lòng có mặt và Check-in trước hoặc đúng giờ. Nếu trễ quá 15 phút, phòng sẽ bị tự động hủy (No Show).</p>
              </div>
              <p style="margin-top:20px;">Trân trọng,<br><strong>Hệ thống Thư viện FPT University</strong></p>
            </div>
          </div>
        </body></html>
        """
        .formatted(patronName, roomName, date, timeSlot);
  }

  private String buildStudyRoomNoShowBody(String patronName, String roomName, String date, String timeSlot) {
    return """
        <html><body style="font-family: Arial, sans-serif; color: #333;">
          <div style="max-width:600px; margin:auto; border:1px solid #e0e0e0; border-radius:8px; overflow:hidden;">
            <div style="background:#f44336; padding:20px; text-align:center;">
              <h2 style="color:#fff; margin:0;">📚 Thư viện FPT University</h2>
            </div>
            <div style="padding:24px;">
              <p>Xin chào <strong>%s</strong>,</p>
              <p>Lịch đặt phòng học nhóm của bạn đã bị <strong style="color:#f44336;">Hủy tự động</strong> do bạn không thực hiện Check-in đúng hạn (trễ quá 15 phút).</p>
              <table style="width:100%%; border-collapse:collapse; margin:16px 0;">
                <tr style="background:#f5f5f5;">
                   <td style="padding:10px; border:1px solid #ddd;"><strong>🏠 Phòng</strong></td>
                   <td style="padding:10px; border:1px solid #ddd;">%s</td>
                </tr>
                <tr>
                   <td style="padding:10px; border:1px solid #ddd;"><strong>📅 Ngày</strong></td>
                   <td style="padding:10px; border:1px solid #ddd;">%s</td>
                </tr>
                <tr style="background:#f5f5f5;">
                   <td style="padding:10px; border:1px solid #ddd;"><strong>⏰ Khung giờ</strong></td>
                   <td style="padding:10px; border:1px solid #ddd;">%s</td>
                </tr>
              </table>
              <p>Phòng hiện đã được chuyển sang trạng thái trống để phục vụ cho các nhóm khác.</p>
              <p style="margin-top:20px;">Trân trọng,<br><strong>Hệ thống Thư viện FPT University</strong></p>
            </div>
          </div>
        </body></html>
        """
        .formatted(patronName, roomName, date, timeSlot);
  }

  private String buildStudyRoomCancellationBody(String patronName, String roomName, String date, String timeSlot) {
    return """
        <html><body style="font-family: Arial, sans-serif; color: #333;">
          <div style="max-width:600px; margin:auto; border:1px solid #e0e0e0; border-radius:8px; overflow:hidden;">
            <div style="background:#003580; padding:20px; text-align:center;">
              <h2 style="color:#fff; margin:0;">📚 Thư viện FPT University</h2>
            </div>
            <div style="padding:24px;">
              <p>Xin chào <strong>%s</strong>,</p>
              <p>Lịch đặt phòng học nhóm của bạn đã được <strong>hủy thành công</strong>.</p>
              <table style="width:100%%; border-collapse:collapse; margin:16px 0;">
                <tr style="background:#f5f5f5;">
                   <td style="padding:10px; border:1px solid #ddd;"><strong>🏠 Phòng</strong></td>
                   <td style="padding:10px; border:1px solid #ddd;">%s</td>
                </tr>
                <tr>
                   <td style="padding:10px; border:1px solid #ddd;"><strong>📅 Ngày</strong></td>
                   <td style="padding:10px; border:1px solid #ddd;">%s</td>
                </tr>
                <tr style="background:#f5f5f5;">
                   <td style="padding:10px; border:1px solid #ddd;"><strong>⏰ Khung giờ</strong></td>
                   <td style="padding:10px; border:1px solid #ddd;">%s</td>
                </tr>
              </table>
              <p style="margin-top:20px;">Trân trọng,<br><strong>Hệ thống Thư viện FPT University</strong></p>
            </div>
          </div>
        </body></html>
        """.formatted(patronName, roomName, date, timeSlot);
  }

  // ==========================================
  // BOOK TRANSFER NOTIFICATIONS
  // ==========================================

  @Override
  public void sendBookTransferDecision(String toEmail, String librarianName, String bookTitle, String sourceCampus,
      String status) {
    String subject = "Accepted".equals(status) ? "[Thư viện FPT] Yêu cầu luân chuyển sách đã ĐƯỢC DUYỆT"
        : "[Thư viện FPT] Yêu cầu luân chuyển sách bị TỪ CHỐI";
    String body = buildBookTransferDecisionBody(librarianName, bookTitle, sourceCampus, status);
    sendHtmlEmail(toEmail, subject, body);
  }

  @Override
  public void sendBookTransferReceiptConfirmation(String toEmail, String librarianName, String bookTitle,
      String destinationCampus) {
    String subject = "[Thư viện FPT] Sách luân chuyển đã đến cơ sở đích";
    String body = buildBookTransferReceiptConfirmBody(librarianName, bookTitle, destinationCampus);
    sendHtmlEmail(toEmail, subject, body);
  }

  private String buildBookTransferDecisionBody(String librarianName, String bookTitle, String sourceCampus,
      String status) {
    boolean isAccepted = "Accepted".equals(status) || "InTransit".equals(status);
    String headerBg = isAccepted ? "#4caf50" : "#f44336";
    String statusText = isAccepted ? "<strong style='color:#4caf50;'>Đã Phê Duyệt & Đang Luân Chuyển</strong>"
        : "<strong style='color:#f44336;'>Đã Bị Từ Chối</strong>";

    return """
        <html><body style="font-family: Arial, sans-serif; color: #333;">
          <div style="max-width:600px; margin:auto; border:1px solid #e0e0e0; border-radius:8px; overflow:hidden;">
            <div style="background:%s; padding:20px; text-align:center;">
              <h2 style="color:#fff; margin:0;">📚 Thư viện FPT University</h2>
            </div>
            <div style="padding:24px;">
              <p>Xin chào <strong>%s</strong>,</p>
              <p>Lệnh xin luân chuyển sách của bạn gửi tới cơ sở <strong>%s</strong> vừa được xử lý.</p>
              <table style="width:100%%; border-collapse:collapse; margin:16px 0;">
                <tr style="background:#f5f5f5;">
                   <td style="padding:10px; border:1px solid #ddd;"><strong>📖 Tên sách / Mã bản sao</strong></td>
                   <td style="padding:10px; border:1px solid #ddd;">%s</td>
                </tr>
                <tr>
                   <td style="padding:10px; border:1px solid #ddd;"><strong>📌 Trạng thái</strong></td>
                   <td style="padding:10px; border:1px solid #ddd;">%s</td>
                </tr>
              </table>
              <p style="margin-top:20px;">Trân trọng,<br><strong>Hệ thống Thư viện FPT University</strong></p>
            </div>
          </div>
        </body></html>
        """.formatted(headerBg, librarianName, sourceCampus, bookTitle, statusText);
  }

  private String buildBookTransferReceiptConfirmBody(String librarianName, String bookTitle, String destinationCampus) {
    return """
        <html><body style="font-family: Arial, sans-serif; color: #333;">
          <div style="max-width:600px; margin:auto; border:1px solid #e0e0e0; border-radius:8px; overflow:hidden;">
            <div style="background:#003580; padding:20px; text-align:center;">
              <h2 style="color:#fff; margin:0;">📚 Thư viện FPT University</h2>
            </div>
            <div style="padding:24px;">
              <p>Xin chào <strong>%s</strong>,</p>
              <p>Một lệnh luân chuyển sách đã được xác nhận <strong>nhận hàng thành công</strong> tại cơ sở đích (<strong>%s</strong>).</p>
              <table style="width:100%%; border-collapse:collapse; margin:16px 0;">
                <tr style="background:#f5f5f5;">
                   <td style="padding:10px; border:1px solid #ddd;"><strong>📖 Tên sách / Mã bản sao</strong></td>
                   <td style="padding:10px; border:1px solid #ddd;">%s</td>
                </tr>
                <tr>
                   <td style="padding:10px; border:1px solid #ddd;"><strong>📌 Trạng thái</strong></td>
                   <td style="padding:10px; border:1px solid #ddd;"><strong style='color:#003580;'>Đã Nhập Kho (Received)</strong></td>
                </tr>
              </table>
              <div style="background:#e8f5e9; border-left:4px solid #4caf50; padding:12px; border-radius:4px;">
                <p style="margin:0;">✅ Chu trình luân chuyển đã hoàn tất. Sách hiện đã có sẵn trên kệ của cơ sở đích.</p>
              </div>
              <p style="margin-top:20px;">Trân trọng,<br><strong>Hệ thống Thư viện FPT University</strong></p>
            </div>
          </div>
        </body></html>
        """
        .formatted(librarianName, destinationCampus, bookTitle);
  }
}
