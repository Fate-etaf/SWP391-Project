package com.swp5.library_management.repository;

import com.swp5.library_management.entity.BorrowTicketDetail;
import com.swp5.library_management.entity.FineInvoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface FineInvoiceRepository extends JpaRepository<FineInvoice, Integer> {

    Optional<FineInvoice> findFirstByTicketDetailAndViolationTypeOrderByCreatedAtDesc(
            BorrowTicketDetail ticketDetail,
            String violationType
    );

    default Optional<FineInvoice> findByTicketDetailAndViolationType(
            BorrowTicketDetail ticketDetail,
            String violationType) {
        return findFirstByTicketDetailAndViolationTypeOrderByCreatedAtDesc(ticketDetail, violationType);
    }

    // Tổng tiền phạt ĐÃ THU trong khoảng thời gian
    @Query("SELECT COALESCE(SUM(f.fineAmount - f.remainingAmount), 0) FROM FineInvoice f " +
           "JOIN f.ticketDetail d WHERE d.borrowTicket.campus.campusId = :campusId " +
           "AND f.paidStatus = 'Paid' AND f.paidAt BETWEEN :startDate AND :endDate")
    BigDecimal sumFinesCollectedInPeriod(@Param("campusId") Integer campusId,
                                         @Param("startDate") LocalDateTime startDate,
                                         @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COUNT(f) FROM FineInvoice f WHERE f.patron.userId = :patronId AND f.paidStatus = 'Unpaid'")
    int countUnpaidFinesByPatronId(@Param("patronId") String patronId);

    // Tổng tiền phạt CHƯA THU (Tính đến hiện tại)
    @Query("SELECT COALESCE(SUM(f.remainingAmount), 0) FROM FineInvoice f " +
           "JOIN f.ticketDetail d WHERE d.borrowTicket.campus.campusId = :campusId " +
           "AND f.paidStatus = 'Unpaid'")
    BigDecimal sumFinesPending(@Param("campusId") Integer campusId);

    /**
     * Lấy danh sách hóa đơn phạt, lọc theo patronId và paidStatus.
     * Truyền null để bỏ qua bộ lọc tương ứng.
     */
    @Query("SELECT f FROM FineInvoice f " +
           "LEFT JOIN FETCH f.patron " +
           "LEFT JOIN FETCH f.ticketDetail td " +
           "LEFT JOIN FETCH td.bookCopy bc " +
           "LEFT JOIN FETCH bc.book " +
           "WHERE (:patronId IS NULL OR f.patron.userId = :patronId) " +
           "AND (:paidStatus IS NULL OR f.paidStatus = :paidStatus) " +
           "ORDER BY f.createdAt DESC")
    List<FineInvoice> findAllFiltered(@Param("patronId") String patronId,
                                      @Param("paidStatus") String paidStatus);

    /** Lấy tất cả hóa đơn phạt còn nợ (Unpaid) của một bạn đọc */
    List<FineInvoice> findByPatronUserIdAndPaidStatus(String patronUserId, String paidStatus);
    // Tính tổng số lượng hóa đơn phạt chưa đóng theo danh sách user
    @Query("SELECT f.patron.userId, COUNT(f) FROM FineInvoice f WHERE f.patron.userId IN :userIds AND UPPER(f.paidStatus) = 'UNPAID' GROUP BY f.patron.userId")
    java.util.List<Object[]> countUnpaidFinesByUsers(@Param("userIds") java.util.List<String> userIds);
}
