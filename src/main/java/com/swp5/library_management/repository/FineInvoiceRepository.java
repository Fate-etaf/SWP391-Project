package com.swp5.library_management.repository;

import com.swp5.library_management.entity.BorrowTicketDetail;
import com.swp5.library_management.entity.FineInvoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FineInvoiceRepository extends JpaRepository<FineInvoice, Integer> {

    Optional<FineInvoice> findByTicketDetailAndViolationType(
            BorrowTicketDetail ticketDetail,
            String violationType
    );
}
