package com.swp5.library_management.repository;

import com.swp5.library_management.entity.BorrowTicket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BorrowTicketRepository extends JpaRepository<BorrowTicket, Integer> {
}
