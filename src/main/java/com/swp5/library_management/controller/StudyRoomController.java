package com.swp5.library_management.controller;

import com.swp5.library_management.dto.RoomBookingRequestDTO;
import com.swp5.library_management.dto.RoomBookingResponseDTO;
import com.swp5.library_management.dto.StudyRoomDTO;
import com.swp5.library_management.security.CustomUserDetails;
import com.swp5.library_management.service.StudyRoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/study-rooms")
@RequiredArgsConstructor
public class StudyRoomController {

    private final StudyRoomService studyRoomService;

    @GetMapping("/campus/{campusId}/available")
    public ResponseEntity<List<StudyRoomDTO>> getAvailableRooms(@PathVariable Integer campusId,
                                                                @RequestParam(name = "date") java.time.LocalDate date,
                                                                HttpSession session) {
        String userId = (String) session.getAttribute("loggedInUserId");
        return ResponseEntity.ok(studyRoomService.getAvailableRooms(campusId, date, userId));
    }

    @PostMapping("/book")
    public ResponseEntity<?> bookRoom(@RequestBody RoomBookingRequestDTO request, 
                                      HttpSession session) {
        try {
            String userId = (String) session.getAttribute("loggedInUserId");
            if (userId == null) {
                return ResponseEntity.status(401).body("Vui lòng đăng nhập");
            }
            RoomBookingResponseDTO response = studyRoomService.bookRoom(request, userId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/book/{bookingId}/cancel")
    public ResponseEntity<?> cancelBooking(@PathVariable Integer bookingId,
                                           HttpSession session) {
        try {
            // Thủ thư cũng có thể gọi API này qua librarianCancel, nhưng librarianCancel có API riêng ở dưới.
            // API này dùng cho sinh viên tự hủy
            String userId = (String) session.getAttribute("loggedInUserId");
            if (userId == null) {
                return ResponseEntity.status(401).body("Vui lòng đăng nhập");
            }
            studyRoomService.cancelBooking(bookingId, userId);
            return ResponseEntity.ok("Đã hủy đơn đặt phòng thành công");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/book/{bookingId}/librarian-cancel")
    public ResponseEntity<?> librarianCancel(@PathVariable Integer bookingId, HttpSession session) {
        try {
            if (!Boolean.TRUE.equals(session.getAttribute("isLibrarian")) && !Boolean.TRUE.equals(session.getAttribute("isAdmin"))) {
                return ResponseEntity.status(403).body("Chỉ thủ thư mới có quyền thực hiện thao tác này");
            }
            studyRoomService.librarianCancel(bookingId);
            return ResponseEntity.ok("Thủ thư đã hủy đơn đặt phòng");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/book/{bookingId}/checkin")
    public ResponseEntity<?> checkIn(@PathVariable Integer bookingId,
                                     HttpSession session) {
        try {
            if (!Boolean.TRUE.equals(session.getAttribute("isLibrarian")) && !Boolean.TRUE.equals(session.getAttribute("isAdmin"))) {
                return ResponseEntity.status(403).body("Chỉ thủ thư mới có quyền Check-in cho nhóm sinh viên");
            }
            // Mặc dù API trước đây dùng userId của sinh viên, nhưng checkIn chỉ cần bookingId là đủ.
            // Truyền dummy userId hoặc sửa Service để không cần userId
            studyRoomService.checkIn(bookingId, ""); 
            return ResponseEntity.ok("Check-in thành công");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/book/{bookingId}/evict")
    public ResponseEntity<?> evictBooking(@PathVariable Integer bookingId, HttpSession session) {
        try {
            if (!Boolean.TRUE.equals(session.getAttribute("isLibrarian")) && !Boolean.TRUE.equals(session.getAttribute("isAdmin"))) {
                return ResponseEntity.status(403).body("Chỉ thủ thư mới có quyền thực hiện thao tác này");
            }
            studyRoomService.evictBooking(bookingId);
            return ResponseEntity.ok("Đã đánh dấu vi phạm và mời nhóm sinh viên ra khỏi phòng");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/book/{bookingId}/checkout")
    public ResponseEntity<?> checkOut(@PathVariable Integer bookingId,
                                      HttpSession session) {
        try {
            String userId = (String) session.getAttribute("loggedInUserId");
            if (userId == null) {
                return ResponseEntity.status(401).body("Vui lòng đăng nhập");
            }
            studyRoomService.checkOut(bookingId, userId);
            return ResponseEntity.ok("Check-out thành công");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
