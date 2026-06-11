package com.swp5.library_management.service;

import java.util.List;

import com.swp5.library_management.entity.TransferRequest;

public interface TransferService {
    
    // 1. Lấy danh sách tất cả lệnh luân chuyển (Sắp xếp mới nhất lên đầu)
    List<TransferRequest> getAllTransfers();
    
    // 2. Tạo lệnh luân chuyển mới (Bước 1 của Use Case)
    TransferRequest createTransfer(Integer fromCampusId, Integer toCampusId, List<String> copyIds, String requestedByUserId, String note);

    // 3. Cập nhật trạng thái đang giao hàng (Bước 2 của Use Case)
    void markAsInTransit(Integer transferId);

    // 4. Xác nhận nhập kho thành công (Bước 3 của Use Case)
    void confirmReceipt(Integer transferId, String confirmedByUserId);
    
    // 5. Hủy lệnh luân chuyển (Alt 2 của Use Case)
    void cancelTransfer(Integer transferId);

    TransferRequest getTransferById(Integer transferId);
}