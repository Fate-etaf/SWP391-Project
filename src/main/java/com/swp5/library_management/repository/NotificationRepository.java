package com.swp5.library_management.repository;

import com.swp5.library_management.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Integer> {

    /** Lấy thông báo của user, sắp xếp mới nhất trước */
    List<Notification> findByUserUserIdOrderByCreatedAtDesc(String userId);

    /** Đếm thông báo chưa gửi (để batch job xử lý sau) */
    long countByStatus(String status);
    
    /** Đếm số thông báo chưa đọc của 1 user */
    long countByUserUserIdAndReadFalse(String userId);

    /** Đánh dấu tất cả thông báo của 1 user thành đã đọc */
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("UPDATE Notification n SET n.read = true WHERE n.user.userId = :userId AND n.read = false")
    void markAllAsRead(@org.springframework.data.repository.query.Param("userId") String userId);
}
