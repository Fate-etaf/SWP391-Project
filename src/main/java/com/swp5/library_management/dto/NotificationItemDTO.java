package com.swp5.library_management.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationItemDTO {
    private Integer notificationId;
    private boolean read;
    private String notificationType;
    private String title;
    private String content;
    private String createdAtFormatted;
}
