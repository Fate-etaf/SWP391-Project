package com.swp5.library_management.service;

import com.swp5.library_management.entity.BorrowTicketDetail;
import com.swp5.library_management.entity.FineInvoice;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface ViolationService {

    /** Sách đang được mượn (chưa trả) */
    List<BorrowTicketDetail> getBorrowingBooks();

    /** Sách đã trả nhưng quá hạn */
    List<BorrowTicketDetail> getReturnedOverdueBooks();

    long calculateOverdueDays(LocalDate dueDate);

    /** Xác nhận trả sách */
    void returnBook(Integer borrowTicketDetailId, String conditionStatus, String librarianId);

    FineInvoice createOverdueFine(Integer borrowTicketDetailId, String conditionStatus, String librarianId);

    List<FineInvoice> createLostBookFine(Integer borrowTicketDetailId, String notes, String librarianId);

    List<FineInvoice> createDamagedBookFine(Integer borrowTicketDetailId, String notes, String librarianId);

    /** Trả sách qua mã Barcode (CopyID) */
    BorrowTicketDetail returnByCopyId(String copyId, String librarianId);

    /**
     * Lấy danh sách tất cả hóa đơn phạt, có thể lọc theo mã bạn đọc và trạng thái.
     * 
     * @param patronId   mã sinh viên (để null nếu không lọc)
     * @param paidStatus trạng thái ('Paid' / 'Unpaid', null nếu không lọc)
     */
    List<FineInvoice> getAllFineInvoices(String patronId, String paidStatus, String librarianId);

    /**
     * Thủ thư xác nhận thu tiền mặt cho hóa đơn phạt.
     * Cập nhật trạng thái hóa đơn và tự động mở khóa thẻ mượn nếu sinh viên hết nợ.
     * 
     * @param fineId      ID hóa đơn phạt
     * @param librarianId mã thủ thư đang xử lý
     */
    void collectFineCash(Integer fineId, String librarianId);

    /**
     * Thủ thư xác nhận thu tiền qua QR Code cho hóa đơn phạt.
     * 
     * @param fineId          ID hóa đơn phạt
     * @param librarianId     mã thủ thư đang xử lý
     * @param transactionCode mã giao dịch đối chiếu
     */
    void collectFineQR(Integer fineId, String librarianId, String transactionCode);

    /** Lấy hóa đơn phạt theo ID */
    FineInvoice getFineInvoiceById(Integer fineId);

    /** Lấy giá gốc của sách theo TicketDetailId */
    BigDecimal getBookPriceByTicketDetailId(Integer ticketDetailId);
}
