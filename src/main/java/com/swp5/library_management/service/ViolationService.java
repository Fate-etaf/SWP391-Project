package com.swp5.library_management.service;

import com.swp5.library_management.entity.BorrowTicketDetail;
import com.swp5.library_management.entity.FineInvoice;

import java.time.LocalDate;
import java.util.List;

public interface ViolationService {

    /** Sách đang được mượn (chưa trả) */
    List<BorrowTicketDetail> getBorrowingBooks();

    /** Sách đã trả nhưng quá hạn */
    List<BorrowTicketDetail> getReturnedOverdueBooks();

    long calculateOverdueDays(LocalDate dueDate);

    /** Xác nhận trả sách */
    void returnBook(Integer borrowTicketDetailId);

    FineInvoice createOverdueFine(Integer borrowTicketDetailId);

    FineInvoice createLostBookFine(Integer borrowTicketDetailId);

    FineInvoice createDamagedBookFine(Integer borrowTicketDetailId);

    /** Trả sách qua mã Barcode (CopyID) */
    BorrowTicketDetail returnByCopyId(String copyId);
}
