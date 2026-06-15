package com.swp5.library_management.repository;

import com.swp5.library_management.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;


import java.util.List;


public interface NotificationRepository extends JpaRepository<Notification, Integer> {

    /** Lấy thông báo của user, sắp xếp mới nhất trước */
    List<Notification> findByUserUserIdOrderByCreatedAtDesc(String userId);

    /** Đếm thông báo chưa gửi (để batch job xử lý sau) */
    long countByStatus(String status);
}
