package com.swp5.library_management.dto;

import lombok.*;

import java.time.LocalDateTime;

/**
 * Kết quả trả về sau khi xử lý đặt giữ chỗ / đăng ký waitlist.
 * UCR06 – ReservationService trả về DTO này cho Controller xử lý View.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservationResultDTO {

    /** true = thành công, false = thất bại */
    private boolean success;

    /** Loại kết quả: RESERVED | WAITLISTED | ERROR */
    private String resultType;

    /** Thông báo hiển thị cho bạn đọc */
    private String message;

    // ── Thông tin khi đặt giữ chỗ thành công (RESERVED) ──────────────────────

    private Integer reservationId;

    /** Thời hạn giữ chỗ (24h sau khi đặt) */
    private LocalDateTime expirationDate;

    // ── Thông tin khi đăng ký waitlist thành công (WAITLISTED) ───────────────

    private Integer waitlistId;

    /** Số thứ tự trong hàng đợi */
    private long waitlistPosition;
}
