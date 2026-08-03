package com.swp5.library_management.job;

import com.swp5.library_management.entity.RoomBooking;
import com.swp5.library_management.entity.User;
import com.swp5.library_management.repository.RoomBookingRepository;
import com.swp5.library_management.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class RoomBookingCleanupJob {

    private final RoomBookingRepository roomBookingRepository;
    private final UserRepository userRepository;
    private final com.swp5.library_management.service.EmailService emailService;

    @Scheduled(fixedRate = 60000) // Chạy mỗi phút
    @Transactional
    public void cleanupNoShowBookings() {
        LocalTime timeMinus15Mins = LocalTime.now().minusMinutes(15);
        List<RoomBooking> noShows = roomBookingRepository.findNoShowBookings(timeMinus15Mins);

        for (RoomBooking booking : noShows) {
            log.info("Booking {} marked as NoShow", booking.getBookingId());
            booking.setStatus("NoShow");
            
            try {
                emailService.sendStudyRoomNoShow(
                        booking.getPatron().getEmail(),
                        booking.getPatron().getFullName(),
                        booking.getStudyRoom().getRoomName(),
                        booking.getBookingDate().toString(),
                        booking.getStartTime() + " - " + booking.getEndTime()
                );
            } catch (Exception e) {
                log.error("Failed to send NoShow email for booking {}", booking.getBookingId(), e);
            }
        }

        if (!noShows.isEmpty()) {
            roomBookingRepository.saveAll(noShows);
        }
    }
}
