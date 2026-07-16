package com.swp5.library_management.service;

import com.swp5.library_management.dto.RoomBookingRequestDTO;
import com.swp5.library_management.dto.RoomBookingResponseDTO;
import com.swp5.library_management.dto.StudyRoomDTO;
import com.swp5.library_management.entity.RoomBooking;
import com.swp5.library_management.entity.StudyRoom;
import com.swp5.library_management.entity.User;
import com.swp5.library_management.repository.RoomBookingRepository;
import com.swp5.library_management.repository.StudyRoomRepository;
import com.swp5.library_management.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudyRoomServiceImpl implements StudyRoomService {

    private final StudyRoomRepository studyRoomRepository;
    private final RoomBookingRepository roomBookingRepository;
    private final UserRepository userRepository;
    private final QrCodeService qrCodeService;
    // private final EmailService emailService; // Temporarily omitted to avoid changing too many files unless needed

    @Override
    public List<StudyRoomDTO> getAvailableRooms(Integer campusId) {
        return studyRoomRepository.findByCampus_CampusIdAndStatus(campusId, "Available")
                .stream()
                .map(room -> StudyRoomDTO.builder()
                        .roomId(room.getRoomId())
                        .roomName(room.getRoomName())
                        .capacity(room.getCapacity())
                        .description(room.getDescription())
                        .status(room.getStatus())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public RoomBookingResponseDTO bookRoom(RoomBookingRequestDTO request, String patronId) {
        // Validate user
        User patron = userRepository.findById(patronId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin bạn đọc"));
        if (Boolean.TRUE.equals(patron.getBorrowingLocked()) || !"Active".equals(patron.getStatus())) {
            throw new RuntimeException("Tài khoản của bạn đang bị khóa quyền đặt phòng");
        }

        // Validate date
        LocalDate today = LocalDate.now();
        if (request.getBookingDate().isBefore(today) || request.getBookingDate().isAfter(today.plusDays(1))) {
            throw new RuntimeException("Chỉ được phép book phòng cho hôm nay hoặc ngày mai");
        }
        if (request.getBookingDate().getDayOfWeek().getValue() > 5) {
            throw new RuntimeException("Phòng học nhóm chỉ phục vụ từ Thứ 2 đến Thứ 6");
        }

        // Validate time
        LocalTime start = request.getStartTime();
        LocalTime end = request.getEndTime();
        if (start.isBefore(LocalTime.of(8, 30)) || end.isAfter(LocalTime.of(17, 0))) {
            throw new RuntimeException("Khung giờ phục vụ là 08:30 - 17:00");
        }
        if (!start.isBefore(end)) {
            throw new RuntimeException("Giờ bắt đầu phải nhỏ hơn giờ kết thúc");
        }
        if (Duration.between(start, end).toMinutes() > 120) {
            throw new RuntimeException("Thời gian book tối đa là 2 giờ/lần");
        }

        // Validate participant count
        if (request.getParticipantCount() < 4 || request.getParticipantCount() > 8) {
            throw new RuntimeException("Số lượng thành viên phải từ 4 đến 8 người");
        }

        // Validate 1 booking per day
        List<RoomBooking> activeBookings = roomBookingRepository.findActiveBookingsByUserAndDate(patronId, request.getBookingDate());
        if (!activeBookings.isEmpty()) {
            throw new RuntimeException("Mỗi nhóm chỉ được book 1 ca/ngày");
        }

        // Validate overlapping
        boolean overlapping = roomBookingRepository.existsOverlappingBooking(
                request.getRoomId(), request.getBookingDate(), start, end);
        if (overlapping) {
            throw new RuntimeException("Rất tiếc, khung giờ bạn chọn vừa được người khác đặt. Vui lòng chọn khung giờ khác.");
        }

        // Get Room
        StudyRoom room = studyRoomRepository.findById(request.getRoomId())
                .orElseThrow(() -> new RuntimeException("Phòng học không tồn tại"));
        if (request.getParticipantCount() > room.getCapacity()) {
            throw new RuntimeException("Số lượng người vượt quá sức chứa của phòng");
        }

        // Save booking
        RoomBooking booking = RoomBooking.builder()
                .studyRoom(room)
                .patron(patron)
                .bookingDate(request.getBookingDate())
                .startTime(start)
                .endTime(end)
                .purpose(request.getPurpose())
                .participantCount(request.getParticipantCount())
                .status("Confirmed")
                .createdAt(LocalDateTime.now())
                .build();
        booking = roomBookingRepository.save(booking);

        // Generate QR code
        String qrContent = booking.getBookingId() + "-" + patron.getUserId();
        byte[] qrBytes = qrCodeService.generatePng(qrContent, 300);
        booking.setQrCode(qrBytes);
        roomBookingRepository.save(booking);
        
        // Return response
        return RoomBookingResponseDTO.builder()
                .bookingId(booking.getBookingId())
                .roomName(room.getRoomName())
                .patronId(patron.getUserId())
                .bookingDate(booking.getBookingDate())
                .startTime(booking.getStartTime())
                .endTime(booking.getEndTime())
                .participantCount(booking.getParticipantCount())
                .purpose(booking.getPurpose())
                .status(booking.getStatus())
                .createdAt(booking.getCreatedAt())
                .qrCodeBase64(qrBytes != null ? Base64.getEncoder().encodeToString(qrBytes) : null)
                .build();
    }

    @Override
    @Transactional
    public void cancelBooking(Integer bookingId, String patronId) {
        RoomBooking booking = roomBookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn đặt phòng"));
        if (!booking.getPatron().getUserId().equals(patronId)) {
            throw new RuntimeException("Bạn không có quyền hủy đơn đặt phòng này");
        }
        if (!"Confirmed".equals(booking.getStatus())) {
            throw new RuntimeException("Chỉ có thể hủy đơn đang ở trạng thái chờ sử dụng (Confirmed)");
        }
        if (booking.getBookingDate().isEqual(LocalDate.now()) && LocalTime.now().isAfter(booking.getStartTime())) {
            throw new RuntimeException("Không thể hủy đặt phòng khi thời gian sử dụng đã bắt đầu");
        }
        
        booking.setStatus("Cancelled");
        roomBookingRepository.save(booking);
    }

    @Override
    @Transactional
    public void checkIn(Integer bookingId, String patronId) {
        RoomBooking booking = roomBookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Mã QR không hợp lệ (Không tìm thấy đơn đặt phòng)"));
        
        if (!"Confirmed".equals(booking.getStatus())) {
            throw new RuntimeException("Đơn đặt phòng không ở trạng thái hợp lệ để Check-in");
        }
        if (!booking.getBookingDate().isEqual(LocalDate.now())) {
            throw new RuntimeException("Lịch đặt phòng không phải ngày hôm nay");
        }
        
        LocalTime now = LocalTime.now();
        LocalTime startAllowed = booking.getStartTime().minusMinutes(15);
        LocalTime endAllowed = booking.getStartTime().plusMinutes(15);
        
        if (now.isBefore(startAllowed)) {
            throw new RuntimeException("Chưa đến giờ nhận phòng (chỉ được nhận trước 15 phút)");
        }
        if (now.isAfter(endAllowed)) {
            // Technically handled by cleanup job, but double check here
            booking.setStatus("NoShow");
            roomBookingRepository.save(booking);
            throw new RuntimeException("Đã quá 15 phút, đơn đặt phòng của bạn đã bị hủy");
        }
        
        booking.setStatus("CheckedIn");
        roomBookingRepository.save(booking);
    }

    @Override
    @Transactional
    public void checkOut(Integer bookingId, String patronId) {
        RoomBooking booking = roomBookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn đặt phòng"));
        if (!"CheckedIn".equals(booking.getStatus())) {
            throw new RuntimeException("Chỉ có thể Check-out khi phòng đang ở trạng thái CheckedIn");
        }
        booking.setStatus("CheckedOut");
        roomBookingRepository.save(booking);
    }

    @Override
    @Transactional
    public void librarianCancel(Integer bookingId) {
        RoomBooking booking = roomBookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn đặt phòng"));
        // Librarian can cancel if rules violated
        booking.setStatus("Cancelled");
        // Also could lock patron: booking.getPatron().setBorrowingLocked(true)
        User patron = booking.getPatron();
        patron.setBorrowingLocked(true);
        userRepository.save(patron);
        roomBookingRepository.save(booking);
    }
}
