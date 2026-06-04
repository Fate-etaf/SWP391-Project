package com.swp5.library_management.service;

/**
 * Service gửi email thông báo.
 * UCR06 bước 8: Gửi mail xác nhận đặt giữ chỗ / hủy đặt chỗ.
 */
public interface EmailService {

    /**
     * Gửi email xác nhận đặt giữ chỗ sách thành công.
     *
     * @param toEmail      Địa chỉ email của bạn đọc
     * @param patronName   Tên bạn đọc
     * @param bookTitle    Tên sách
     * @param campusName   Tên cơ sở thư viện nhận sách
     * @param expiryInfo   Thông tin thời hạn giữ chỗ (ví dụ: "trước 20:00 ngày 01/06/2026")
     */
    void sendReservationConfirmation(String toEmail, String patronName,
                                     String bookTitle, String campusName,
                                     String expiryInfo);

    /**
     * Gửi email xác nhận hủy đặt giữ chỗ.
     *
     * @param toEmail    Địa chỉ email của bạn đọc
     * @param patronName Tên bạn đọc
     * @param bookTitle  Tên sách bị hủy đặt
     */
    void sendReservationCancellation(String toEmail, String patronName, String bookTitle);

    /**
     * Gửi email xác nhận đã vào hàng đợi waitlist.
     *
     * @param toEmail      Địa chỉ email của bạn đọc
     * @param patronName   Tên bạn đọc
     * @param bookTitle    Tên sách
     * @param position     Số thứ tự trong hàng chờ
     */
    void sendWaitlistConfirmation(String toEmail, String patronName,
                                  String bookTitle, long position);

    /**
     * Gửi email xác nhận hủy xếp hàng chờ thành công.
     *
     * @param toEmail    Địa chỉ email của bạn đọc
     * @param patronName Tên bạn đọc
     * @param bookTitle  Tên sách bị hủy xếp hàng chờ
     */
    void sendWaitlistCancellation(String toEmail, String patronName, String bookTitle);

    /**
     * Gửi email thông báo mượn sách thành công (khi thủ thư tạo phiếu mượn).
     *
     * @param toEmail    Địa chỉ email của bạn đọc
     * @param patronName Tên bạn đọc
     * @param bookTitle  Tên sách được mượn
     * @param copyId     Mã bản sao sách vật lý
     * @param dueDate    Hạn trả sách (ví dụ: "10/06/2026")
     */
    void sendLoanConfirmation(String toEmail, String patronName, String bookTitle, String copyId, String dueDate);
}
