package com.swp5.library_management.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.swp5.library_management.entity.BorrowTicket;


public interface BorrowTicketRepository extends JpaRepository<BorrowTicket, Integer> {
    int countByPatronUserId(String patronId);
}
