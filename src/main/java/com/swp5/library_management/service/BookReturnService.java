package com.swp5.library_management.service;

import com.swp5.library_management.entity.BorrowTicketDetail;
import java.util.List;
import java.util.Map;

public interface BookReturnService {
    List<BorrowTicketDetail> getCurrentlyBorrowing();
    List<BorrowTicketDetail> searchCurrentlyBorrowing(String title, String borrowerId);
    Map<String, Object> checkScan(String copyId);
    void processNormalReturn(Integer ticketDetailId);
    void processOverdueReturn(Integer ticketDetailId, String paymentMethod, String transactionCode, String librarianId);
    void processLost(Integer ticketDetailId, boolean payNow, String paymentMethod, String transactionCode, String librarianId);
    void processDamaged(Integer ticketDetailId, boolean payNow, String paymentMethod, String transactionCode, String librarianId);
}
