package com.swp5.library_management.service;

/**
 * Service gửi email thông báo.
 * UCR06 bước 8: Gửi mail xác nhận đặt giữ chỗ / hủy đặt chỗ.
 */
public interface EmailService {

        /**
         * Gửi email xác nhận đặt giữ chỗ sách thành công.
         *
         * @param toEmail    Địa chỉ email của bạn đọc
         * @param patronName Tên bạn đọc
         * @param bookTitle  Tên sách
         * @param campusName Tên cơ sở thư viện nhận sách
         * @param expiryInfo Thông tin thời hạn giữ chỗ (ví dụ: "trước 20:00 ngày
         *                   01/06/2026")
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
         * @param toEmail    Địa chỉ email của bạn đọc
         * @param patronName Tên bạn đọc
         * @param bookTitle  Tên sách
         * @param position   Số thứ tự trong hàng chờ
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

        /**
         * Gửi email xác nhận đã nhận đơn đề nghị tài liệu mới.
         *
         * @param toEmail    Địa chỉ email của bạn đọc
         * @param patronName Tên bạn đọc
         * @param bookTitle  Tên sách đề nghị
         * @param author     Tác giả sách đề nghị
         * @param priority   Độ ưu tiên (Low, Medium, High)
         */
        void sendMaterialRequestConfirmation(String toEmail, String patronName, String bookTitle, String author,
                        String priority);

        /**
         * Gửi email xác nhận yêu cầu đề nghị tài liệu đã được duyệt.
         *
         * @param toEmail    Địa chỉ email của bạn đọc
         * @param patronName Tên bạn đọc
         * @param bookTitle  Tên sách đề nghị
         * @param author     Tác giả sách đề nghị
         */
        void sendMaterialRequestApproval(String toEmail, String patronName, String bookTitle, String author);

        /**
         * Gửi email từ chối yêu cầu đề nghị tài liệu.
         *
         * @param toEmail    Địa chỉ email của bạn đọc
         * @param patronName Tên bạn đọc
         * @param bookTitle  Tên sách đề nghị
         * @param author     Tác giả sách đề nghị
         */
        void sendMaterialRequestRejection(String toEmail, String patronName, String bookTitle, String author);

        /**
         * Gửi email thông báo mã OTP kích hoạt hoặc đặt lại mật khẩu tài khoản.
         *
         * @param toEmail Địa chỉ email của bạn đọc
         * @param subject Tiêu đề email
         * @param content Nội dung thông báo (chứa mã OTP)
         */
        void sendOtpEmail(String toEmail, String subject, String content);

        /**
         * Gửi email xác nhận trả sách và ghi nhận các khoản phạt / thanh toán nếu có.
         *
         * @param toEmail       Địa chỉ email bạn đọc
         * @param patronName    Tên bạn đọc
         * @param bookTitle     Tên sách trả
         * @param copyId        Mã bản sao
         * @param returnDate    Thời điểm nhận trả
         * @param fineAmount    Số tiền phạt
         * @param paymentMethod Phương thức thanh toán
         * @param status        Trạng thái trả sách
         */
        void sendReturnConfirmation(String toEmail, String patronName, String bookTitle, String copyId,
                        String returnDate, String fineAmount, String paymentMethod, String status);

        // ==========================================
        // STUDY ROOM NOTIFICATIONS
        // ==========================================

        /**
         * Gửi email xác nhận đặt phòng học nhóm thành công.
         */
        void sendStudyRoomBookingConfirmation(String toEmail, String patronName, String roomName, String date,
                        String timeSlot);

        /**
         * Gửi email thông báo hủy phòng do không đến nhận phòng (No Show).
         */
        void sendStudyRoomNoShow(String toEmail, String patronName, String roomName, String date, String timeSlot);

        /**
         * Gửi email xác nhận hủy đặt phòng học nhóm.
         */
        void sendStudyRoomCancellation(String toEmail, String patronName, String roomName, String date,
                        String timeSlot);

        // ==========================================
        // BOOK TRANSFER NOTIFICATIONS
        // ==========================================

        /**
         * Gửi email thông báo quyết định luân chuyển sách (Chấp nhận/Từ chối).
         */
        void sendBookTransferDecision(String toEmail, String librarianName, String bookTitle, String sourceCampus,
                        String status);

        /**
         * Gửi email thông báo sách luân chuyển đã được nhận thành công.
         */
        void sendBookTransferReceiptConfirmation(String toEmail, String librarianName, String bookTitle,
                        String destinationCampus);
}
