package com.swp5.library_management.service;

import com.swp5.library_management.dto.ReservationResultDTO;
import com.swp5.library_management.entity.Reservation;
import com.swp5.library_management.entity.Waitlist;

import java.util.List;

/**
 * Service xử lý Use Case UCR06 – Reserve Book Online.
 */
public interface ReservationService {

    /**
     * [Normal Flow] Bạn đọc đặt giữ chỗ sách tại cơ sở thư viện chỉ định.
     *
     * Kiểm tra theo thứ tự:
     * 1. Tài khoản hợp lệ, không bị khóa (Exc 1)
     * 2. Chưa vượt giới hạn 3 đơn active (Exc 2)
     * 3. Có bản sách Available tại campus (Exc 3 → waitlist)
     * 4. Lock bản sách, tạo đơn (Exc 4 → race condition → waitlist)
     * 5. Ghi Notification + gửi Email
     *
     * @param patronId       Mã bạn đọc (hiện tại hardcoded, sau đổi thành Session)
     * @param bookId         ID đầu sách muốn đặt
     * @param pickupCampusId ID cơ sở thư viện muốn đến nhận
     * @return ReservationResultDTO chứa kết quả (RESERVED / WAITLISTED / ERROR)
     */
    ReservationResultDTO reserveBook(String patronId, Integer bookId, Integer pickupCampusId);

    /**
     * [Alt 1] Bạn đọc hủy đơn đặt giữ chỗ đang Holding.
     * Cập nhật trạng thái đơn → Cancelled, trả BookCopy → Available.
     *
     * @param patronId      Mã bạn đọc (để verify quyền sở hữu đơn)
     * @param reservationId ID đơn đặt giữ chỗ cần hủy
     */
    void cancelReservation(String patronId, Integer reservationId);

    /**
     * Lấy danh sách đặt chỗ của một bạn đọc.
     */
    List<Reservation> getMyReservations(String patronId);

    /**
     * Lấy danh sách hàng đợi chờ sách của một bạn đọc.
     */
    List<Waitlist> getMyWaitlists(String patronId);

    /**
     * [Exc 3 / Exc 4] Đăng ký vào hàng đợi waitlist khi hết sách.
     *
     * @param patronId  Mã bạn đọc
     * @param bookId    ID đầu sách
     * @param campusId  ID cơ sở thư viện
     * @return ReservationResultDTO với resultType = WAITLISTED và vị trí hàng chờ
     */
    ReservationResultDTO joinWaitlist(String patronId, Integer bookId, Integer campusId);

    /**
     * [Alt 2] Bạn đọc hủy đăng ký xếp hàng chờ.
     * Cập nhật trạng thái hàng chờ thành Cancelled.
     *
     * @param patronId   Mã bạn đọc (để verify quyền sở hữu)
     * @param waitlistId ID hàng chờ cần hủy
     */
    void cancelWaitlist(String patronId, Integer waitlistId);
}
