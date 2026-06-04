package com.swp5.library_management.repository;

import com.swp5.library_management.entity.BorrowTicketDetail;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BorrowTicketDetailRepository extends JpaRepository<BorrowTicketDetail, Integer> {

    @EntityGraph(attributePaths = {"bookCopy", "bookCopy.book", "borrowTicket", "borrowTicket.patron"})
    @Query("SELECT b FROM BorrowTicketDetail b " +
            "WHERE b.returnDate IS NULL " +
            "AND b.dueDate < :now " +
            "AND (b.status IS NULL OR b.status NOT IN ('Lost', 'Damaged'))")
    List<BorrowTicketDetail> findActiveOverdue(@Param("now") LocalDateTime now);

    @EntityGraph(attributePaths = {"bookCopy", "bookCopy.book", "bookCopy.book.authors", "borrowTicket", "returnCampus"})
    @Query("SELECT d FROM BorrowTicketDetail d WHERE d.borrowTicket.patron.userId = :patronId ORDER BY d.borrowTicket.createdAt DESC")
    List<BorrowTicketDetail> findHistoryByPatronId(@Param("patronId") String patronId);

    @Query("SELECT COUNT(d) FROM BorrowTicketDetail d WHERE d.borrowTicket.patron.userId = :patronId AND d.returnDate IS NULL")
    int countActiveBorrowedByPatronId(@Param("patronId") String patronId);
}

