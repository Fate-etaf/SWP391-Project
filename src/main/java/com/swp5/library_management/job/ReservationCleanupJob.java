package com.swp5.library_management.job;

import com.swp5.library_management.entity.BookCopy;
import com.swp5.library_management.entity.Notification;
import com.swp5.library_management.entity.Reservation;
import com.swp5.library_management.repository.BookCopyRepository;
import com.swp5.library_management.repository.NotificationRepository;
import com.swp5.library_management.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationCleanupJob {

    private final ReservationRepository reservationRepository;
    private final BookCopyRepository bookCopyRepository;
    private final NotificationRepository notificationRepository;
    private final com.swp5.library_management.service.ReservationService reservationService;

    /**
     * Chạy tự động vào 0h00 mỗi ngày để dọn dẹp các đơn đặt chỗ quá hạn.
     */
    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void cleanupExpiredReservations() {
        log.info("[CRON] Bắt đầu dọn dẹp các đơn đặt chỗ (Reservation) quá hạn...");

        LocalDateTime now = LocalDateTime.now();

        // Tìm các đơn Holding có expirationDate < hiện tại
        List<Reservation> expiredReservations = reservationRepository.findByStatusAndExpirationDateBefore("Holding", now);

        if (expiredReservations.isEmpty()) {
            log.info("[CRON] Không có đơn đặt chỗ nào quá hạn cần dọn dẹp.");
            return;
        }

        int count = 0;
        for (Reservation res : expiredReservations) {
            res.setStatus("Expired");
            reservationRepository.save(res);

            // Hoàn lại sách
            BookCopy copy = res.getCopy();
            if (copy != null) {
                boolean assignedToWaitlist = reservationService.processWaitlistForReturnedBook(copy);
                if (!assignedToWaitlist) {
                    copy.setCopyStatus("Available");
                    bookCopyRepository.save(copy);
                }
            }

            // Ghi thông báo
            Notification notif = Notification.builder()
                    .user(res.getPatron())
                    .notificationType("RESERVATION_EXPIRED")
                    .title("Đơn đặt giữ chỗ quá hạn")
                    .content(String.format("Đơn đặt giữ chỗ #%d cho cuốn sách \"%s\" đã bị hủy do bạn không đến nhận đúng hạn.",
                            res.getReservationId(), res.getBook().getTitle()))
                    .status("Pending")
                    .createdAt(now)
                    .build();
            notificationRepository.save(notif);

            count++;
        }

        log.info("[CRON] Đã hoàn tất dọn dẹp. Tổng số đơn quá hạn bị hủy: {}", count);
    }
}
