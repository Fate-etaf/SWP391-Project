package com.swp5.library_management.repository;

import com.swp5.library_management.entity.Waitlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


import java.util.List;


public interface WaitlistRepository extends JpaRepository<Waitlist, Integer> {

    /**
     * Exc 3 (UCR06): Kiểm tra bạn đọc đã có trong hàng đợi chưa (tránh đăng ký trùng).
     */
    boolean existsByBookBookIdAndPatronUserIdAndStatusIn(Integer bookId, String patronId, List<String> statuses);

    /**
     * Exc 3 (UCR06): Đếm số người đang đứng trước bạn đọc trong hàng chờ
     * để hiển thị số thứ tự.
     */
    @Query("""
            SELECT COUNT(w)
            FROM Waitlist w
            WHERE w.book.bookId = :bookId
              AND w.campus.campusId = :campusId
              AND w.status IN ('Waiting', 'Notified')
              AND w.requestedAt < (
                  SELECT w2.requestedAt FROM Waitlist w2
                  WHERE w2.waitlistId = :waitlistId
              )
            """)
    long countAheadInQueue(@Param("bookId") Integer bookId,
                           @Param("campusId") Integer campusId,
                           @Param("waitlistId") Integer waitlistId);

    long countByBookBookIdAndCampusCampusIdAndStatusIn(Integer bookId, Integer campusId, List<String> statuses);

    long countByBookBookIdAndStatusIn(Integer bookId, List<String> statuses);

    /**
     * Lấy danh sách Waitlist của một bạn đọc
     */
    List<Waitlist> findByPatronUserIdOrderByRequestedAtDesc(String patronId);

    /**
     * Lấy danh sách đang chờ (Waiting) của 1 cuốn sách tại 1 cơ sở, xếp theo thứ tự ưu tiên thời gian
     */
    @Query("SELECT w FROM Waitlist w WHERE w.book.bookId = :bookId AND w.campus.campusId = :campusId AND w.status = 'Waiting' ORDER BY w.requestedAt ASC")
    List<Waitlist> findWaitingListByBookAndCampus(@Param("bookId") Integer bookId, @Param("campusId") Integer campusId);
}
