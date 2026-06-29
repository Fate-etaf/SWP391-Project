package com.swp5.library_management.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.swp5.library_management.entity.TransferRequest;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TransferRequestRepository extends JpaRepository<TransferRequest, Integer>, JpaSpecificationExecutor<TransferRequest> {
    // Lấy tất cả lệnh luân chuyển, sắp xếp mới nhất lên đầu
    @Query("SELECT t FROM TransferRequest t JOIN FETCH t.fromCampus JOIN FETCH t.toCampus ORDER BY t.requestedAt DESC")
    List<TransferRequest> findAllWithCampusesOrderByRequestedAtDesc();

    /**
     * Dashboard Top Card: Đếm số yêu cầu xin sách TỪ cơ sở này đang chờ duyệt (cần xuất kho)
     */
    @Query("SELECT COUNT(t) FROM TransferRequest t WHERE t.fromCampus.campusId = :campusId AND t.status = 'Pending'")
    long countPendingOutboundRequests(@Param("campusId") Integer campusId);

    // Working Board: Outbound (Pending, Accepted, Rejected)
    List<TransferRequest> findByFromCampusCampusIdAndStatusInOrderByRequestedAtDesc(Integer campusId, List<String> statuses);

    // Working Board: Inbound (InTransit)
    List<TransferRequest> findByToCampusCampusIdAndStatusOrderByShippedAtDesc(Integer campusId, String status);

    // Working Board: My Sent Requests (Pending, Accepted, Rejected)
    List<TransferRequest> findByToCampusCampusIdAndStatusInOrderByRequestedAtDesc(Integer campusId, List<String> statuses);
}
