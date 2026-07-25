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


    // 5. Giao cho vận chuyển (Đổi sang InTransit) - Bổ sung tham số thứ 2 để kiểm
    // tra quyền
    void markAsInTransit(Integer transferId, Integer librarianCampusId, String userId);

    // 6. Nhập kho (Postcondition: Cập nhật CampusID và đưa sách về Available)
    void confirmReceipt(Integer transferId, String confirmedByUserId);

    // ==========================================
    // KANBAN WORKING BOARD & HISTORY NEW METHODS
    // ==========================================

    // Lấy danh sách Request đang xử lý (Outbound: Pending, Accepted, Rejected)
    List<TransferRequest> getWorkingOutboundRequests(Integer campusId);

    // Lấy danh sách Yêu cầu đã gửi đi (My Sent Requests: Pending, Accepted, Rejected)
    List<TransferRequest> getMyPendingRequestsToOtherCampuses(Integer campusId);

    // Lấy danh sách Lô hàng đang giao đến (Inbound: InTransit)
    List<TransferRequest> getWorkingInboundRequests(Integer campusId);

    // Thay đổi trạng thái Request (Accept/Reject) với kiểm tra bảo mật (chỉ fromCampus được duyệt)
    void updateRequestStatus(Integer transferId, String status, String note, Integer librarianCampusId);

    // Batch Confirm: Xác nhận xuất kho hàng loạt cho tất cả các đơn Accepted
    void confirmBatchShipment(Integer campusId, String userId);

    // Batch Receive: Xác nhận nhập kho hàng loạt theo ToCampus và ShippedAt
    void confirmBatchReceipt(Integer toCampusId, java.time.LocalDateTime shippedAt, String confirmedByUserId);
    
    // Hủy Transfer (Chỉ dành cho Pending)
    void cancelTransfer(Integer transferId, Integer requesterCampusId);

    // Lịch sử luân chuyển
    org.springframework.data.domain.Page<TransferRequest> getTransferHistory(com.swp5.library_management.dto.TransferFilterDTO filter);
}