package com.swp5.library_management.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.swp5.library_management.entity.BorrowTicketDetail;
import com.swp5.library_management.entity.FineInvoice;

@Repository
public interface FineInvoiceRepository extends JpaRepository<FineInvoice, Integer> {

    Optional<FineInvoice> findByTicketDetailAndViolationType(
            BorrowTicketDetail ticketDetail,
            String violationType
    );

    // Tổng tiền phạt ĐÃ THU trong khoảng thời gian
    @Query("SELECT COALESCE(SUM(f.fineAmount - f.remainingAmount), 0) FROM FineInvoice f JOIN f.ticketDetail d WHERE d.borrowTicket.campus.campusId = :campusId AND f.paidStatus = 'Paid' AND f.paidAt BETWEEN :startDate AND :endDate")
    BigDecimal sumFinesCollectedInPeriod(@Param("campusId") Integer campusId, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    // Tổng tiền phạt CHƯA THU (Tính đến hiện tại)
    @Query("SELECT COALESCE(SUM(f.remainingAmount), 0) FROM FineInvoice f JOIN f.ticketDetail d WHERE d.borrowTicket.campus.campusId = :campusId AND f.paidStatus = 'Unpaid'")
    BigDecimal sumFinesPending(@Param("campusId") Integer campusId);
}
