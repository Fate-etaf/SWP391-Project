package com.swp5.library_management.controller;

import com.swp5.library_management.dto.NotificationDTO;
import com.swp5.library_management.service.NotificationService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationRestController {

    private final NotificationService notificationService;

    public NotificationRestController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getNotifications(HttpSession session) {
        String userId = (String) session.getAttribute("loggedInUserId");
        if (userId == null) {
            Map<String, Object> err = new HashMap<>();
            err.put("error", "UserId is null in session");
            return ResponseEntity.status(401).body(err);
        }

        List<NotificationDTO> top10 = notificationService.getTop10Notifications(userId);
        long unreadCount = notificationService.getUnreadCount(userId);

        Map<String, Object> response = new HashMap<>();
        response.put("notifications", top10);
        response.put("unreadCount", unreadCount);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Integer id, HttpSession session) {
        String userId = (String) session.getAttribute("loggedInUserId");
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }

        notificationService.markAsRead(id, userId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(HttpSession session) {
        String userId = (String) session.getAttribute("loggedInUserId");
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }

        notificationService.markAllAsRead(userId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/test-error")
    public ResponseEntity<Map<String, Object>> testError() {
        try {
            List<NotificationDTO> top10 = notificationService.getTop10Notifications("LB00001");
            long unreadCount = notificationService.getUnreadCount("LB00001");
            Map<String, Object> response = new HashMap<>();
            response.put("notifications", top10);
            response.put("unreadCount", unreadCount);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> err = new HashMap<>();
            err.put("error", e.getMessage());
            err.put("cause", e.getCause() != null ? e.getCause().getMessage() : "null");
            e.printStackTrace();
            return ResponseEntity.status(500).body(err);
        }
    }
}
