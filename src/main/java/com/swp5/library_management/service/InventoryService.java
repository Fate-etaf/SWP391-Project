package com.swp5.library_management.service;

import com.swp5.library_management.dto.InventoryOverviewDTO;

public interface InventoryService {
    InventoryOverviewDTO getOverview();
    InventoryOverviewDTO getStats(Integer campusId, Integer categoryId, String fromDate, String toDate);
}
