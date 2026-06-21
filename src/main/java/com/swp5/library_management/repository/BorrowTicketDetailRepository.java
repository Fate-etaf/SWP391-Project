package com.swp5.library_management.repository;

import com.swp5.library_management.entity.BorrowTicketDetail;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface BorrowTicketDetailRepository extends JpaRepository<BorrowTicketDetail, Integer> {

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

       /** Tìm bản ghi mượn "đang hoạt động" của 1 CopyID cụ thể (để trả nhanh bằng Barcode) */
       @EntityGraph(attributePaths = { "bookCopy", "bookCopy.book", "borrowTicket", "borrowTicket.patron" })
       @Query("SELECT b FROM BorrowTicketDetail b " +
                     "WHERE b.bookCopy.copyId = :copyId " +
                     "AND b.returnDate IS NULL " +
                     "AND (b.status IS NULL OR b.status NOT IN ('Returned', 'Lost', 'Damaged'))")
       List<BorrowTicketDetail> findActiveByCopyId(String copyId);
}
