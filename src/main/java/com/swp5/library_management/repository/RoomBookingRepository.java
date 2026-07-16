package com.swp5.library_management.repository;

import com.swp5.library_management.entity.RoomBooking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Repository
public interface RoomBookingRepository extends JpaRepository<RoomBooking, Integer> {
    
    // Tìm các đơn đặt phòng của 1 User trong 1 ngày, không tính các đơn đã hủy/vắng mặt
    @Query("SELECT rb FROM RoomBooking rb WHERE rb.patron.userId = :userId AND rb.bookingDate = :date AND rb.status NOT IN ('Cancelled', 'NoShow')")
    List<RoomBooking> findActiveBookingsByUserAndDate(@Param("userId") String userId, @Param("date") LocalDate date);

    // Lấy tất cả đơn đặt phòng của 1 phòng trong 1 ngày cụ thể
    @Query("SELECT rb FROM RoomBooking rb WHERE rb.studyRoom.roomId = :roomId AND rb.bookingDate = :date AND rb.status NOT IN ('Cancelled', 'NoShow', 'Evicted')")
    List<RoomBooking> findActiveBookingsByRoomAndDate(@Param("roomId") Integer roomId, @Param("date") LocalDate date);
    
    // Kiểm tra xem có đơn đặt phòng nào trùng lặp với thời gian yêu cầu không
    @Query("SELECT CASE WHEN COUNT(rb) > 0 THEN true ELSE false END FROM RoomBooking rb " +
           "WHERE rb.studyRoom.roomId = :roomId AND rb.bookingDate = :date AND rb.status NOT IN ('Cancelled', 'NoShow', 'Evicted') " +
           "AND (rb.startTime < :endTime AND rb.endTime > :startTime)")
    boolean existsOverlappingBooking(@Param("roomId") Integer roomId, @Param("date") LocalDate date, 
                                     @Param("startTime") LocalTime startTime, @Param("endTime") LocalTime endTime);
                                     
    // Tìm đơn đặt phòng của user theo status
    List<RoomBooking> findByPatron_UserIdOrderByBookingDateDescStartTimeDesc(String userId);

    // Tìm các đơn đang Confirmed và đã quá giờ StartTime 15 phút (cho cleanup job)
    @Query("SELECT rb FROM RoomBooking rb WHERE rb.status = 'Confirmed' AND " +
           "(rb.bookingDate < CURRENT_DATE OR (rb.bookingDate = CURRENT_DATE AND rb.startTime <= :timeMinus15Mins))")
    List<RoomBooking> findNoShowBookings(@Param("timeMinus15Mins") LocalTime timeMinus15Mins);
}
