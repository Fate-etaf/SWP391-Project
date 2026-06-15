package com.swp5.library_management.service;

import com.swp5.library_management.dto.NotificationDTO;
import com.swp5.library_management.entity.Notification;
import com.swp5.library_management.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public List<NotificationDTO> getTop10Notifications(String userId) {
        // Here we just fetch all and limit to 10 for simplicity, 
        // ideally should use Pageable repository method.
        List<Notification> all = notificationRepository.findByUserUserIdOrderByCreatedAtDesc(userId);
        return all.stream()
                .limit(10)
                .map(NotificationDTO::new)
                .collect(Collectors.toList());
    }

    public long getUnreadCount(String userId) {
        return notificationRepository.countByUserUserIdAndReadFalse(userId);
    }

    @Transactional
    public void markAsRead(Integer notificationId, String userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElse(null);
        if (notification != null && notification.getUser().getUserId().equals(userId)) {
            notification.setRead(true);
            notificationRepository.save(notification);
        }
    }

    @Transactional
    public void markAllAsRead(String userId) {
        notificationRepository.markAllAsRead(userId);
    }
}
