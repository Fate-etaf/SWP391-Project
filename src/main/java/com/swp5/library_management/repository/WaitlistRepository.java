package com.swp5.library_management.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.swp5.library_management.dto.DashboardDataDTO.WaitlistHotspotDTO;
import com.swp5.library_management.entity.Waitlist;

public interface WaitlistRepository extends JpaRepository<Waitlist, Integer> {

    /**
     * Exc 3 (UCR06): Kiểm tra bạn đọc đã có trong hàng đợi chưa (tránh đăng ký
     * trùng).
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
     /**
      * Dashboard: Lấy Top các đầu sách đang có nhiều người xếp hàng nhất tại 1 cơ sở
      */
    @Query("SELECT new com.swp5.library_management.dto.DashboardDataDTO$WaitlistHotspotDTO(" +
            "w.book.bookId, w.book.title, w.book.isbn, COUNT(w)) " +
            "FROM Waitlist w " +
            "WHERE w.campus.campusId = :campusId AND w.status = 'Waiting' " +
            "GROUP BY w.book.bookId, w.book.title, w.book.isbn " +
            "ORDER BY COUNT(w) DESC")
    List<WaitlistHotspotDTO> findTopWaitlistHotspots(@Param("campusId") Integer campusId, Pageable pageable);

    @Query("SELECT new com.swp5.library_management.dto.WaitlistHotspotDTO(" +
            "w.book.bookId, w.book.title, w.book.isbn, w.campus.campusId, w.campus.campusName, COUNT(w)) " +
            "FROM Waitlist w " +
            "WHERE w.campus.campusId != :currentCampusId AND w.status = 'Waiting' " +
            "GROUP BY w.book.bookId, w.book.title, w.book.isbn, w.campus.campusId, w.campus.campusName " +
            "ORDER BY COUNT(w) DESC")
    List<com.swp5.library_management.dto.WaitlistHotspotDTO> findSuggestedTransfers(@Param("currentCampusId") Integer currentCampusId);

     /**
      * Lấy danh sách đang chờ (Waiting) của 1 cuốn sách tại 1 cơ sở, xếp theo thứ tự ưu tiên thời gian
      */
    @Query("SELECT w FROM Waitlist w WHERE w.book.bookId = :bookId AND w.campus.campusId = :campusId AND w.status = 'Waiting' ORDER BY w.requestedAt ASC")
    List<Waitlist> findWaitingListByBookAndCampus(@Param("bookId") Integer bookId, @Param("campusId") Integer campusId);
}
