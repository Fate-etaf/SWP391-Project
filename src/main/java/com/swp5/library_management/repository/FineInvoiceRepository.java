package com.swp5.library_management.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.swp5.library_management.entity.BorrowTicketDetail;
import com.swp5.library_management.entity.FineInvoice;


public interface FineInvoiceRepository extends JpaRepository<FineInvoice, Integer> {

    Optional<FineInvoice> findByTicketDetailAndViolationType(
            BorrowTicketDetail ticketDetail,
            String violationType
    );

    // Tổng tiền phạt ĐÃ THU trong khoảng thời gian
    @Query("SELECT COALESCE(SUM(f.fineAmount - f.remainingAmount), 0) FROM FineInvoice f JOIN f.ticketDetail d WHERE d.borrowTicket.campus.campusId = :campusId AND f.paidStatus = 'Paid' AND f.paidAt BETWEEN :startDate AND :endDate")
    BigDecimal sumFinesCollectedInPeriod(@Param("campusId") Integer campusId, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COUNT(f) FROM FineInvoice f WHERE f.patron.userId = :patronId AND f.paidStatus = 'Unpaid'")
    int countUnpaidFinesByPatronId(@Param("patronId") String patronId);

    // Tổng tiền phạt CHƯA THU (Tính đến hiện tại)
    @Query("SELECT COALESCE(SUM(f.remainingAmount), 0) FROM FineInvoice f JOIN f.ticketDetail d WHERE d.borrowTicket.campus.campusId = :campusId AND f.paidStatus = 'Unpaid'")
    BigDecimal sumFinesPending(@Param("campusId") Integer campusId);

    // Tính tổng số lượng hóa đơn phạt chưa đóng theo danh sách user
    @Query("SELECT f.patron.userId, COUNT(f) FROM FineInvoice f WHERE f.patron.userId IN :userIds AND UPPER(f.paidStatus) = 'UNPAID' GROUP BY f.patron.userId")
    java.util.List<Object[]> countUnpaidFinesByUsers(@Param("userIds") java.util.List<String> userIds);
}
