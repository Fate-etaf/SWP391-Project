package com.swp5.library_management.reader.service;

import com.swp5.library_management.reader.dto.BorrowingHistoryDTO;
import java.util.List;

public interface BorrowingService {
    List<BorrowingHistoryDTO> getBorrowingHistory(String patronId);
}
