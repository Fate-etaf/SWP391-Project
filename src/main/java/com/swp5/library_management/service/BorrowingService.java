package com.swp5.library_management.service;

import com.swp5.library_management.dto.BorrowingHistoryDTO;
import java.util.List;

public interface BorrowingService {
    List<BorrowingHistoryDTO> getBorrowingHistory(String patronId);
}
