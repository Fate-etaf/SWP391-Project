package com.swp5.library_management.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Map bảng Notifications – thông báo nội bộ hệ thống.
 * UCR06 bước 8: Ghi thông báo sau khi đặt/hủy giữ chỗ thành công.
 */
@Entity
@Table(name = "Notifications", schema = "dbo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "NotificationID")
    private Integer notificationId;

    /** Người nhận thông báo */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "UserID", nullable = false)
    private User user;

    /** Loại thông báo: RESERVATION_CONFIRMED, RESERVATION_CANCELLED, WAITLIST_JOINED, v.v. */
    @Column(name = "NotificationType", nullable = false, length = 50)
    private String notificationType;

    @Column(name = "Title", nullable = false, length = 200)
    private String title;

    @Column(name = "Content", nullable = false, columnDefinition = "NVARCHAR(MAX)")
    private String content;

    /**
     * Trạng thái gửi:
     * Pending – chờ gửi
     * Sent    – đã gửi thành công
     * Failed  – gửi thất bại
     */
    @Column(name = "Status", nullable = false, length = 20)
    @Builder.Default
    private String status = "Pending";

    @Column(name = "SentAt")
    private LocalDateTime sentAt;

    @Column(name = "CreatedAt", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    /** Trạng thái đã xem trên web UI */
    @Column(name = "IsRead", nullable = false)
    @Builder.Default
    private boolean read = false;
}
