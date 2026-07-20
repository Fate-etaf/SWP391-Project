package com.swp5.library_management.service;

import com.swp5.library_management.dto.DashboardDataDTO;
import com.swp5.library_management.dto.InventoryOverviewDTO;

public interface InventoryService {
    InventoryOverviewDTO getOverview();

    InventoryOverviewDTO getStats(Integer campusId, Integer categoryId, String fromDate, String toDate);

    /**
     * Lấy toàn bộ dữ liệu Real-time cho Librarian Inventory Dashboard.
     * 
     * @param campusId        ID cơ sở (Bắt buộc, được inject từ session của Thủ
     *                        thư)
     * @param subjectCode     Mã môn học (Tùy chọn filter)
     * @param conditionStatus Tình trạng vật lý (Tùy chọn filter)
     * @param copyStatus      Trạng thái mượn trả (Tùy chọn để xoay biểu đồ
     *                        Doughnut)
     */
    DashboardDataDTO getDashboardData(Integer campusId, java.util.List<String> subjectCodes, java.util.List<String> conditions, java.util.List<String> statuses);
}
