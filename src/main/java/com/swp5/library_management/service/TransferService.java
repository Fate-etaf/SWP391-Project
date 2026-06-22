package com.swp5.library_management.service;

import java.util.List;

import com.swp5.library_management.entity.TransferRequest;

public interface TransferService {
    // 1. Xem danh sách
    List<TransferRequest> getAllTransfers();

    // 2. Xem chi tiết
    TransferRequest getTransferById(Integer transferId);

    // 3. Tạo lệnh xuất kho (Precondition: Sách Available & Thuộc cơ sở người tạo)
    void createTransfer(Integer fromCampusId, Integer toCampusId, List<String> copyIds, String requestedByUserId,
            String note);

    // 4. Hủy lệnh (Alt 2: Trả sách về Available)
    void cancelTransfer(Integer transferId);

    // 5. Giao cho vận chuyển (Đổi sang InTransit) - Bổ sung tham số thứ 2 để kiểm
    // tra quyền
    void markAsInTransit(Integer transferId, Integer librarianCampusId);

    // 6. Nhập kho (Postcondition: Cập nhật CampusID và đưa sách về Available)
    void confirmReceipt(Integer transferId, String confirmedByUserId);
}