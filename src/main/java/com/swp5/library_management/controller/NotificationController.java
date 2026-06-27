package com.swp5.library_management.controller;

import com.swp5.library_management.dto.NotificationItemDTO;
import com.swp5.library_management.dto.NotificationResponseDTO;
import com.swp5.library_management.entity.Notification;
import com.swp5.library_management.entity.User;
import com.swp5.library_management.repository.NotificationRepository;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationRepository notificationRepository;
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");

    private String getLoggedInUserId(HttpSession session) {
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser != null) {
            return loggedInUser.getUserId();
        }
        return (String) session.getAttribute("loggedInUserId");
    }

    @GetMapping
    public ResponseEntity<NotificationResponseDTO> getNotifications(HttpSession session) {
        String userId = getLoggedInUserId(session);
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }

        List<Notification> notifs = notificationRepository.findByUserUserIdOrderByCreatedAtDesc(userId);
        long unreadCount = notificationRepository.countByUserUserIdAndReadFalse(userId);

        List<NotificationItemDTO> items = notifs.stream().map(n -> NotificationItemDTO.builder()
                .notificationId(n.getNotificationId())
                .read(n.isRead())
                .notificationType(n.getNotificationType())
                .title(n.getTitle())
                .content(n.getContent())
                .createdAtFormatted(n.getCreatedAt() != null ? n.getCreatedAt().format(formatter) : "")
                .build()).collect(Collectors.toList());

        return ResponseEntity.ok(new NotificationResponseDTO(unreadCount, items));
    }

    @PostMapping("/{id}/read")
    @Transactional
    public ResponseEntity<Void> markAsRead(@PathVariable("id") Integer id, HttpSession session) {
        String userId = getLoggedInUserId(session);
        if (userId == null) return ResponseEntity.status(401).build();

        notificationRepository.findById(id).ifPresent(n -> {
            if (n.getUser() != null && n.getUser().getUserId().equals(userId)) {
                n.setRead(true);
                notificationRepository.save(n);
            }
        });
        return ResponseEntity.ok().build();
    }

    @PostMapping("/read-all")
    @Transactional
    public ResponseEntity<Void> markAllAsRead(HttpSession session) {
        String userId = getLoggedInUserId(session);
        if (userId == null) return ResponseEntity.status(401).build();

        notificationRepository.markAllAsRead(userId);
        return ResponseEntity.ok().build();
    }
}
