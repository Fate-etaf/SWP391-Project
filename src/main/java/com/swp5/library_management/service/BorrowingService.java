package com.swp5.library_management.service;

import java.util.List;

import com.swp5.library_management.dto.BorrowingHistoryDTO;

public interface BorrowingService {
    List<BorrowingHistoryDTO> getBorrowingHistory(String patronId);
}
