package com.swp5.library_management.service;

import com.swp5.library_management.entity.BorrowTicketDetail;
import java.util.List;
import java.util.Map;

public interface BookReturnService {
    List<BorrowTicketDetail> getCurrentlyBorrowing();

    List<BorrowTicketDetail> searchCurrentlyBorrowing(String title, String borrowerId, String librarianId);

    Map<String, Object> checkScan(String copyId);

    void processNormalReturn(Integer ticketDetailId, String conditionStatus);

    void processOverdueReturn(Integer ticketDetailId, String paymentMethod, String transactionCode, String librarianId, String conditionStatus);

    void processLost(Integer ticketDetailId, boolean payNow, String paymentMethod, String transactionCode,
            String librarianId, String notes);

    void processDamaged(Integer ticketDetailId, boolean payNow, String paymentMethod, String transactionCode,
            String librarianId, String notes);

    boolean isBookReturned(Integer ticketDetailId);

    void registerActiveLibrarian(Integer ticketDetailId, String librarianId);

    String getActiveLibrarian(Integer ticketDetailId);
}
