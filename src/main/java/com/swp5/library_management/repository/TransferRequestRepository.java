package com.swp5.library_management.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.swp5.library_management.entity.TransferRequest;

public interface TransferRequestRepository extends JpaRepository<TransferRequest, Integer> {
    // Lấy tất cả lệnh luân chuyển, sắp xếp mới nhất lên đầu
    @Query("SELECT t FROM TransferRequest t JOIN FETCH t.fromCampus JOIN FETCH t.toCampus ORDER BY t.requestedAt DESC")
    List<TransferRequest> findAllWithCampusesOrderByRequestedAtDesc();

    /**
     * Dashboard Top Card: Đếm số yêu cầu xin sách TỪ cơ sở này đang chờ duyệt (cần xuất kho)
     */
    @Query("SELECT COUNT(t) FROM TransferRequest t WHERE t.fromCampus.campusId = :campusId AND t.status = 'Pending'")
    long countPendingOutboundRequests(@Param("campusId") Integer campusId);
}
