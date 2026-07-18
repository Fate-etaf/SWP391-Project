package com.swp5.library_management.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Map bảng Reservations – đơn đặt giữ chỗ sách.
 * UCR06: Reserve Book Online
 */
@Entity
@Table(name = "Reservations", schema = "dbo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ReservationID")
    private Integer reservationId;

    /** Bạn đọc đặt chỗ */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PatronID", nullable = false)
    private User patron;

    /** Đầu sách được đặt giữ chỗ */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "BookID", nullable = false)
    private Book book;

    /** Bản sách vật lý cụ thể bị khóa (có thể null khi mới tạo) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CopyID")
    private BookCopy copy;

    /** Cơ sở thư viện bạn đọc muốn đến nhận sách */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PickupCampusID", nullable = false)
    private Campus pickupCampus;

    @Column(name = "ReservedAt", nullable = false)
    @Builder.Default
    private LocalDateTime reservedAt = LocalDateTime.now();

    /** Thời hạn giữ chỗ (mặc định 24 giờ từ khi đặt) */
    @Column(name = "ExpirationDate", nullable = false)
    private LocalDateTime expirationDate;

    /**
     * Trạng thái đơn:
     * Holding    – đang giữ chỗ, chờ bạn đọc đến nhận
     * Completed  – bạn đọc đã đến nhận và làm phiếu mượn
     * Cancelled  – bạn đọc chủ động hủy
     * Expired    – quá thời hạn 24h mà không đến nhận
     */
    @Column(name = "Status", nullable = false, length = 20)
    @Builder.Default
    private String status = "Holding";
}
