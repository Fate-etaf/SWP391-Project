package com.swp5.library_management.librarian.service;

import com.swp5.library_management.entity.BorrowTicketDetail;
import com.swp5.library_management.entity.FineInvoice;

import java.time.LocalDate;
import java.util.List;

public interface ViolationService {

    List<BorrowTicketDetail> getOverdueBooks();

    long calculateOverdueDays(LocalDate dueDate);

    FineInvoice createOverdueFine(Integer borrowTicketDetailId);

    FineInvoice createLostBookFine(Integer borrowTicketDetailId);

    FineInvoice createDamagedBookFine(Integer borrowTicketDetailId);
}
