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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
    public String myBookingsPage(@RequestParam(defaultValue = "0") int page, Model model, HttpSession session) {
        String userId = (String) session.getAttribute("loggedInUserId");
        if (userId == null) {
            return "redirect:/login";
        }
        Pageable pageable = PageRequest.of(page, 10);
        Page<RoomBooking> bookingsPage = roomBookingRepository.findByPatron_UserIdOrderByBookingDateDescStartTimeDesc(userId, pageable);
        
        List<BookingViewData> viewData = bookingsPage.getContent().stream().map(b -> new BookingViewData(
                b.getBookingId(),
                b.getStudyRoom().getRoomName(),
                b.getBookingDate(),
                b.getStartTime(),
                b.getEndTime(),
                b.getParticipantCount(),
                b.getStatus(),
                b.getQrCode() != null ? Base64.getEncoder().encodeToString(b.getQrCode()) : null,
                b.getPatron().getUserId(),
                b.getPatron().getFullName(),
                b.getPatron().getEmail()
        )).collect(Collectors.toList());

        model.addAttribute("bookings", viewData);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", bookingsPage.getTotalPages());
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
    public String managementPage(@RequestParam(name = "date", required = false) java.time.LocalDate date, 
                                 @RequestParam(defaultValue = "0") int page, 
                                 Model model, HttpSession session) {
        if (!Boolean.TRUE.equals(session.getAttribute("isLibrarian")) && !Boolean.TRUE.equals(session.getAttribute("isAdmin"))) {
            return "redirect:/login";
        }
        java.time.LocalDate targetDate = date != null ? date : java.time.LocalDate.now();
        model.addAttribute("targetDate", targetDate);
        
        Integer campusId = (Integer) session.getAttribute("loggedInCampusId");
        final Integer finalCampusId = (campusId == null) ? 1 : campusId; // Default
        
        Pageable pageable = PageRequest.of(page, 10);
        Page<RoomBooking> bookingsPage = roomBookingRepository.findByBookingDateAndStudyRoom_Campus_CampusIdOrderByStartTimeDesc(targetDate, finalCampusId, pageable);

        List<BookingViewData> viewData = bookingsPage.getContent().stream().map(b -> new BookingViewData(
                b.getBookingId(),
                b.getStudyRoom().getRoomName(),
                b.getBookingDate(),
                b.getStartTime(),
                b.getEndTime(),
                b.getParticipantCount(),
                b.getStatus(),
                null,
                b.getPatron().getUserId(),
                b.getPatron().getFullName(),
                b.getPatron().getEmail()
        )).collect(Collectors.toList());

        model.addAttribute("bookings", viewData);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", bookingsPage.getTotalPages());
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
        public String patronId;
        public String patronName;
        public String patronEmail;
        
        public BookingViewData(Integer bookingId, String roomName, java.time.LocalDate bookingDate, java.time.LocalTime startTime, java.time.LocalTime endTime, Integer participantCount, String status, String qrBase64, String patronId, String patronName, String patronEmail) {
            this.bookingId = bookingId;
            this.roomName = roomName;
            this.bookingDate = bookingDate;
            this.startTime = startTime;
            this.endTime = endTime;
            this.participantCount = participantCount;
            this.status = status;
            this.qrBase64 = qrBase64;
            this.patronId = patronId;
            this.patronName = patronName;
            this.patronEmail = patronEmail;
        }
    }
}
