package com.swp5.library_management.dto;

import com.swp5.library_management.entity.Notification;
import lombok.Data;

import java.time.format.DateTimeFormatter;

@Data
public class NotificationDTO {
    private Integer notificationId;
    private String notificationType;
    private String title;
    private String content;
    private boolean isRead;
    private String createdAtFormatted;

    public NotificationDTO(Notification entity) {
        this.notificationId = entity.getNotificationId();
        this.notificationType = entity.getNotificationType();
        this.title = entity.getTitle();
        this.content = entity.getContent();
        this.isRead = entity.isRead();
        if (entity.getCreatedAt() != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            this.createdAtFormatted = entity.getCreatedAt().format(formatter);
        }
    }
}
