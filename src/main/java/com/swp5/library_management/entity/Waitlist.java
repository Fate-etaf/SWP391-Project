package com.swp5.library_management.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Map bảng Waitlists – hàng đợi khi hết sách sẵn sàng tại cơ sở.
 * UCR06: Exc 3 – Campus hết sách → Bạn đọc đăng ký xếp hàng chờ
 */
@Entity
@Table(name = "Waitlists", schema = "dbo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Waitlist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "WaitlistID")
    private Integer waitlistId;

    /** Đầu sách muốn chờ */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "BookID", nullable = false)
    private Book book;

    /** Bạn đọc xếp hàng chờ */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PatronID", nullable = false)
    private User patron;

    /** Cơ sở thư viện bạn đọc muốn nhận sách */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CampusID", nullable = false)
    private Campus campus;

    @Column(name = "RequestedAt", nullable = false)
    @Builder.Default
    private LocalDateTime requestedAt = LocalDateTime.now();

    /** Thời điểm hệ thống gửi thông báo cho bạn đọc biết đã có sách */
    @Column(name = "NotifiedAt")
    private LocalDateTime notifiedAt;

    /**
     * Trạng thái:
     * Waiting   – đang chờ
     * Notified  – đã được thông báo có sách (chờ xác nhận/lấy)
     * Converted – đã chuyển thành đơn đặt giữ chỗ thật
     * Cancelled – bạn đọc tự hủy
     */
    @Column(name = "Status", nullable = false, length = 20)
    @Builder.Default
    private String status = "Waiting";

    @Transient
    private Long queuePosition;
}
