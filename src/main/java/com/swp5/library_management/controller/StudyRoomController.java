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

@RestController
@RequestMapping("/api/study-rooms")
@RequiredArgsConstructor
public class StudyRoomController {

    private final StudyRoomService studyRoomService;

    @GetMapping("/campus/{campusId}/available")
    public ResponseEntity<List<StudyRoomDTO>> getAvailableRooms(@PathVariable Integer campusId) {
        return ResponseEntity.ok(studyRoomService.getAvailableRooms(campusId));
    }

    @PostMapping("/book")
    public ResponseEntity<?> bookRoom(@RequestBody RoomBookingRequestDTO request, 
                                      @AuthenticationPrincipal CustomUserDetails userDetails) {
        try {
            if (userDetails == null) {
                return ResponseEntity.status(401).body("Vui lòng đăng nhập");
            }
            RoomBookingResponseDTO response = studyRoomService.bookRoom(request, userDetails.getUser().getUserId());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/book/{bookingId}/cancel")
    public ResponseEntity<?> cancelBooking(@PathVariable Integer bookingId,
                                           @AuthenticationPrincipal CustomUserDetails userDetails) {
        try {
            if (userDetails == null) {
                return ResponseEntity.status(401).body("Vui lòng đăng nhập");
            }
            studyRoomService.cancelBooking(bookingId, userDetails.getUser().getUserId());
            return ResponseEntity.ok("Đã hủy đơn đặt phòng thành công");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/book/{bookingId}/checkin")
    public ResponseEntity<?> checkIn(@PathVariable Integer bookingId,
                                     @AuthenticationPrincipal CustomUserDetails userDetails) {
        try {
            if (userDetails == null) {
                return ResponseEntity.status(401).body("Vui lòng đăng nhập");
            }
            studyRoomService.checkIn(bookingId, userDetails.getUser().getUserId());
            return ResponseEntity.ok("Check-in thành công");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/book/{bookingId}/checkout")
    public ResponseEntity<?> checkOut(@PathVariable Integer bookingId,
                                      @AuthenticationPrincipal CustomUserDetails userDetails) {
        try {
            if (userDetails == null) {
                return ResponseEntity.status(401).body("Vui lòng đăng nhập");
            }
            studyRoomService.checkOut(bookingId, userDetails.getUser().getUserId());
            return ResponseEntity.ok("Check-out thành công");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
