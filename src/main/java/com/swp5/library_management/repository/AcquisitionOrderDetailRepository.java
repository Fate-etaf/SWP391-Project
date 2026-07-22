package com.swp5.library_management.repository;

import com.swp5.library_management.entity.AcquisitionOrderDetail;
import com.swp5.library_management.entity.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AcquisitionOrderDetailRepository extends JpaRepository<AcquisitionOrderDetail, Integer> {

    /**
     * Lấy giá nhập gần nhất của một đầu sách từ đơn đặt mua có trạng thái 'Received'.
     * Sắp xếp theo ngày nhận hàng và OrderDetailID giảm dần để luôn lấy lần nhập gần nhất.
     */
    @Query("SELECT d FROM AcquisitionOrderDetail d " +
           "WHERE d.book = :book AND d.order.status = 'Received' AND d.unitPrice IS NOT NULL " +
           "ORDER BY d.order.receivedDate DESC, d.orderDetailId DESC")
    Optional<AcquisitionOrderDetail> findLatestByBook(@Param("book") Book book);
}
