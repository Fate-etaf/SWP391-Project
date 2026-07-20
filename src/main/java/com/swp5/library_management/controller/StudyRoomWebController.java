package com.swp5.library_management.controller;

import com.swp5.library_management.entity.Campus;
import com.swp5.library_management.entity.RoomBooking;
import com.swp5.library_management.repository.CampusRepository;
import com.swp5.library_management.repository.RoomBookingRepository;
import com.swp5.library_management.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import jakarta.servlet.http.HttpSession;

import java.util.List;
import java.util.Base64;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class StudyRoomWebController {

    private final CampusRepository campusRepository;
    private final RoomBookingRepository roomBookingRepository;

    @GetMapping("/study-rooms")
    public String bookRoomPage(Model model, HttpSession session) {
        if (session.getAttribute("loggedInUserId") == null) {
            return "redirect:/login";
        }
        List<Campus> campuses = campusRepository.findAll();
        model.addAttribute("campuses", campuses);
        model.addAttribute("userCampusId", session.getAttribute("loggedInCampusId"));
        return "study-rooms/book";
    }

    @GetMapping("/study-rooms/my-bookings")
    public String myBookingsPage(Model model, HttpSession session) {
        String userId = (String) session.getAttribute("loggedInUserId");
        if (userId == null) {
            return "redirect:/login";
        }
        List<RoomBooking> bookings = roomBookingRepository.findByPatron_UserIdOrderByBookingDateDescStartTimeDesc(userId);
        
        List<BookingViewData> viewData = bookings.stream().map(b -> new BookingViewData(
                b.getBookingId(),
                b.getStudyRoom().getRoomName(),
                b.getBookingDate(),
                b.getStartTime(),
                b.getEndTime(),
                b.getParticipantCount(),
                b.getStatus(),
                b.getQrCode() != null ? Base64.getEncoder().encodeToString(b.getQrCode()) : null
        )).collect(Collectors.toList());

        model.addAttribute("bookings", viewData);
        return "study-rooms/my-bookings";
    }

    @GetMapping("/librarian/study-room-scanner")
    public String scannerPage(@RequestParam(name = "bookingId", required = false) Integer bookingId, Model model, HttpSession session) {
        if (!Boolean.TRUE.equals(session.getAttribute("isLibrarian")) && !Boolean.TRUE.equals(session.getAttribute("isAdmin"))) {
            return "redirect:/login";
        }
        model.addAttribute("prefillBookingId", bookingId);
        return "librarian/study-room-scanner";
    }

    @GetMapping("/librarian/study-rooms/management")
    public String managementPage(@RequestParam(name = "date", required = false) java.time.LocalDate date, Model model, HttpSession session) {
        if (!Boolean.TRUE.equals(session.getAttribute("isLibrarian")) && !Boolean.TRUE.equals(session.getAttribute("isAdmin"))) {
            return "redirect:/login";
        }
        java.time.LocalDate targetDate = date != null ? date : java.time.LocalDate.now();
        model.addAttribute("targetDate", targetDate);
        
        Integer campusId = (Integer) session.getAttribute("loggedInCampusId");
        final Integer finalCampusId = (campusId == null) ? 1 : campusId; // Default
        
        // Cần import com.swp5.library_management.repository.StudyRoomRepository và entity StudyRoom
        // Nhưng tạm thời ta có thể dùng roomBookingRepository lọc theo status
        // Để không phải khai báo thêm Repository, ta filter trực tiếp từ allBookings
        List<RoomBooking> allBookings = roomBookingRepository.findAll(); 
        List<RoomBooking> filteredBookings = allBookings.stream()
                .filter(b -> b.getBookingDate().isEqual(targetDate))
                .filter(b -> b.getStudyRoom().getCampus().getCampusId().equals(finalCampusId))
                .sorted((b1, b2) -> b2.getStartTime().compareTo(b1.getStartTime()))
                .collect(Collectors.toList());

        List<BookingViewData> viewData = filteredBookings.stream().map(b -> new BookingViewData(
                b.getBookingId(),
                b.getStudyRoom().getRoomName(),
                b.getBookingDate(),
                b.getStartTime(),
                b.getEndTime(),
                b.getParticipantCount(),
                b.getStatus(),
                null
        )).collect(Collectors.toList());

        model.addAttribute("bookings", viewData);
        return "librarian/study-rooms/roomManagement";
    }

    public static class BookingViewData {
        public Integer bookingId;
        public String roomName;
        public java.time.LocalDate bookingDate;
        public java.time.LocalTime startTime;
        public java.time.LocalTime endTime;
        public Integer participantCount;
        public String status;
        public String qrBase64;
        
        public BookingViewData(Integer bookingId, String roomName, java.time.LocalDate bookingDate, java.time.LocalTime startTime, java.time.LocalTime endTime, Integer participantCount, String status, String qrBase64) {
            this.bookingId = bookingId;
            this.roomName = roomName;
            this.bookingDate = bookingDate;
            this.startTime = startTime;
            this.endTime = endTime;
            this.participantCount = participantCount;
            this.status = status;
            this.qrBase64 = qrBase64;
        }
    }
}
