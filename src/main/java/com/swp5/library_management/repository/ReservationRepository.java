package com.swp5.library_management.repository;

import com.swp5.library_management.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


import java.util.List;


public interface ReservationRepository extends JpaRepository<Reservation, Integer> {

    /**
     * Bước 3 (UCR06): Đếm số đơn đang Holding của bạn đọc để kiểm tra giới hạn.
     */
    long countByPatronUserIdAndStatus(String patronId, String status);

    /**
     * Alt 1 (UCR06): Lấy danh sách đơn đang Holding để bạn đọc hủy.
     */
    List<Reservation> findByPatronUserIdAndStatusOrderByReservedAtDesc(String patronId, String status);

    /**
     * Trang cá nhân: Lấy toàn bộ lịch sử đặt chỗ của bạn đọc.
     */
    List<Reservation> findByPatronUserIdOrderByReservedAtDesc(String patronId);

    /**
     * Kiểm tra bạn đọc đã có đơn đặt cho cuốn sách này tại campus này chưa
     * (tránh đặt trùng).
     */
    @Query("""
            SELECT COUNT(r) > 0
            FROM Reservation r
            WHERE r.patron.userId = :patronId
              AND r.book.bookId = :bookId
              AND r.pickupCampus.campusId = :campusId
              AND r.status = 'Holding'
            """)
    boolean existsActiveReservation(@Param("patronId") String patronId,
                                    @Param("bookId") Integer bookId,
                                    @Param("campusId") Integer campusId);

    @Query("SELECT COUNT(r) > 0 FROM Reservation r WHERE r.patron.userId = :patronId AND r.book.bookId = :bookId AND r.status = 'Holding'")
    boolean existsActiveReservationForBook(@Param("patronId") String patronId, @Param("bookId") Integer bookId);

    /**
     * Tìm các đơn giữ chỗ có trạng thái cụ thể và thời gian hết hạn trước một thời điểm nhất định.
     */
    List<Reservation> findByStatusAndExpirationDateBefore(String status, java.time.LocalDateTime date);
}
