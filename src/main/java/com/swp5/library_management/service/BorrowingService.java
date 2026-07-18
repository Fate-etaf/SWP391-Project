package com.swp5.library_management.service;

import java.util.List;

import com.swp5.library_management.dto.BorrowingHistoryDTO;

import com.swp5.library_management.dto.ReservationResultDTO;

public interface BorrowingService {
    List<BorrowingHistoryDTO> getBorrowingHistory(String patronId);
    
    ReservationResultDTO renewBook(String patronId, Integer ticketDetailId);
}
