package com.swp5.library_management.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponseDTO {
    private long unreadCount;
    private List<NotificationItemDTO> notifications;
}
