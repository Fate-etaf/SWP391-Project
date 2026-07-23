package com.swp5.library_management.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.swp5.library_management.entity.BorrowTicketDetail;

public interface BorrowTicketDetailRepository
                extends JpaRepository<BorrowTicketDetail, Integer>, JpaSpecificationExecutor<BorrowTicketDetail> {

        @EntityGraph(attributePaths = { "bookCopy", "bookCopy.book", "borrowTicket", "borrowTicket.patron" })
        @Query("SELECT b FROM BorrowTicketDetail b " +
                        "WHERE b.returnDate IS NULL " +
                        "AND b.dueDate < :now " +
                        "AND (b.status IS NULL OR b.status NOT IN ('Lost', 'Damaged'))")
        List<BorrowTicketDetail> findActiveOverdue(@Param("now") LocalDateTime now);

        @EntityGraph(attributePaths = { "bookCopy", "bookCopy.book", "bookCopy.book.authors", "borrowTicket",
                        "returnCampus" })
        @Query("SELECT d FROM BorrowTicketDetail d WHERE d.borrowTicket.patron.userId = :patronId ORDER BY d.borrowTicket.createdAt DESC")
        List<BorrowTicketDetail> findHistoryByPatronId(@Param("patronId") String patronId);

        @Query("SELECT COUNT(d) FROM BorrowTicketDetail d WHERE d.borrowTicket.patron.userId = :patronId AND d.returnDate IS NULL")
        int countActiveBorrowedByPatronId(@Param("patronId") String patronId);

        // Đếm số lượng sách MƯỢN tại cơ sở trong khoảng thời gian
        @Query("SELECT COUNT(d) FROM BorrowTicketDetail d WHERE d.borrowTicket.campus.campusId = :campusId AND d.borrowTicket.createdAt BETWEEN :startDate AND :endDate")
        long countBorrowedInPeriod(@Param("campusId") Integer campusId, @Param("startDate") LocalDateTime startDate,
                        @Param("endDate") LocalDateTime endDate);

        // Đếm số lượng sách TRẢ tại cơ sở trong khoảng thời gian
        @Query("SELECT COUNT(d) FROM BorrowTicketDetail d WHERE d.borrowTicket.campus.campusId = :campusId AND d.status = 'Returned' AND d.returnDate BETWEEN :startDate AND :endDate")
        long countReturnedInPeriod(@Param("campusId") Integer campusId, @Param("startDate") LocalDateTime startDate,
                        @Param("endDate") LocalDateTime endDate);

        @Query("SELECT COUNT(d) FROM BorrowTicketDetail d WHERE d.borrowTicket.patron.userId = :patronId AND d.returnDate IS NULL AND d.dueDate < CURRENT_TIMESTAMP")
        int countOverdueByPatronId(@Param("patronId") String patronId);
        // Đếm số lượng sách đang QUÁ HẠN (Chưa trả)
        @Query("SELECT COUNT(d) FROM BorrowTicketDetail d WHERE d.borrowTicket.campus.campusId = :campusId AND d.status = 'Overdue' AND d.returnDate IS NULL")
        long countCurrentOverdue(@Param("campusId") Integer campusId);

        @Query("SELECT new com.swp5.library_management.dto.TransactionRecordDTO(" +
                        "b.title, c.copyId, p.fullName, l.fullName, t.createdAt, d.dueDate, d.returnDate, d.status) " +
                        "FROM BorrowTicketDetail d " +
                        "JOIN d.borrowTicket t " +
                        "JOIN t.patron p " +
                        "JOIN t.librarian l " +
                        "JOIN d.bookCopy c " +
                        "JOIN c.book b " +
                        "WHERE t.campus.campusId = :campusId " +
                        "AND (t.createdAt BETWEEN :startDate AND :endDate OR d.returnDate BETWEEN :startDate AND :endDate) "
                        +
                        "ORDER BY t.createdAt DESC")
        java.util.List<com.swp5.library_management.dto.TransactionRecordDTO> getTransactionDetails(
                        @Param("campusId") Integer campusId,
                        @Param("startDate") java.time.LocalDateTime startDate,
                        @Param("endDate") java.time.LocalDateTime endDate);

        /** Sách đang được mượn (chưa trả) — dùng cho trang "Đang mượn" */
        @EntityGraph(attributePaths = { "bookCopy", "bookCopy.book", "borrowTicket", "borrowTicket.patron",
                        "borrowTicket.campus" })
        @Query("SELECT b FROM BorrowTicketDetail b " +
                        "WHERE b.returnDate IS NULL " +
                        "AND (b.status IS NULL OR b.status NOT IN ('Returned', 'Lost', 'Damaged'))")
        List<BorrowTicketDetail> findCurrentlyBorrowing();

        /** Sách đã trả nhưng trả quá hạn — dùng cho trang "Quá hạn" */
        @EntityGraph(attributePaths = { "bookCopy", "bookCopy.book", "borrowTicket", "borrowTicket.patron" })
        @Query("SELECT b FROM BorrowTicketDetail b " +
                        "WHERE b.returnDate IS NOT NULL " +
                        "AND b.dueDate IS NOT NULL " +
                        "AND b.returnDate > b.dueDate " +
                        "AND (b.status IS NULL OR b.status NOT IN ('Lost', 'Damaged'))")
        List<BorrowTicketDetail> findReturnedOverdue();

        /**
         * Tìm bản ghi mượn "đang hoạt động" của 1 CopyID cụ thể (để trả nhanh bằng
         * Barcode)
         */
        @EntityGraph(attributePaths = { "bookCopy", "bookCopy.book", "borrowTicket", "borrowTicket.patron" })
        @Query("SELECT b FROM BorrowTicketDetail b " +
                        "WHERE b.bookCopy.copyId = :copyId " +
                        "AND b.returnDate IS NULL " +
                        "AND (b.status IS NULL OR b.status NOT IN ('Returned', 'Lost', 'Damaged'))")
        List<BorrowTicketDetail> findActiveByCopyId(String copyId);

        /**
         * Dashboard: Lấy danh sách Sách quá hạn cần thu hồi (Bảng Actionable)
         */
        @Query("SELECT new com.swp5.library_management.dto.DashboardDataDTO$OverdueActionDTO(" +
                        "c.copyId, b.title, p.userId, p.fullName, " +
                        "DATEDIFF(DAY, d.dueDate, CURRENT_TIMESTAMP)) " +
                        "FROM BorrowTicketDetail d " +
                        "JOIN d.bookCopy c " +
                        "JOIN c.book b " +
                        "JOIN d.borrowTicket t " +
                        "JOIN t.patron p " +
                        "WHERE t.campus.campusId = :campusId " +
                        "AND d.status = 'Overdue' " +
                        "AND d.returnDate IS NULL " +
                        "ORDER BY d.dueDate ASC")
        List<com.swp5.library_management.dto.DashboardDataDTO.OverdueActionDTO> findOverdueActionsByCampus(
                        @Param("campusId") Integer campusId, org.springframework.data.domain.Pageable pageable);

       @Query("SELECT d.borrowTicket.patron.userId, COUNT(d) FROM BorrowTicketDetail d WHERE d.borrowTicket.patron.userId IN :userIds AND d.returnDate IS NULL AND d.dueDate < CURRENT_TIMESTAMP GROUP BY d.borrowTicket.patron.userId")
       java.util.List<Object[]> countOverdueByUsers(@Param("userIds") java.util.List<String> userIds);

       @Query("SELECT d.borrowTicket.patron.userId, COUNT(d) FROM BorrowTicketDetail d WHERE d.borrowTicket.patron.userId IN :userIds AND d.returnDate IS NULL GROUP BY d.borrowTicket.patron.userId")
       java.util.List<Object[]> countActiveBorrowedByUsers(@Param("userIds") java.util.List<String> userIds);

       @EntityGraph(attributePaths = { "bookCopy", "bookCopy.book", "borrowTicket", "borrowTicket.patron", "borrowTicket.campus" })
       @Query("SELECT b FROM BorrowTicketDetail b " +
              "WHERE b.returnDate IS NULL " +
              "AND (b.status IS NULL OR b.status NOT IN ('Returned', 'Lost', 'Damaged')) " +
              "AND (:title IS NULL OR LOWER(b.bookCopy.book.title) LIKE LOWER(CONCAT('%', :title, '%'))) " +
              "AND (:borrowerId IS NULL OR LOWER(b.borrowTicket.patron.userId) LIKE LOWER(CONCAT('%', :borrowerId, '%')))")
       List<BorrowTicketDetail> searchCurrentlyBorrowing(@Param("title") String title, @Param("borrowerId") String borrowerId);

}

       
