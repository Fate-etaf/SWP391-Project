package com.swp5.library_management.service;

import com.swp5.library_management.dto.RoomBookingRequestDTO;
import com.swp5.library_management.dto.RoomBookingResponseDTO;
import com.swp5.library_management.dto.StudyRoomDTO;

import java.time.LocalDate;
import java.util.List;

public interface StudyRoomService {
    List<StudyRoomDTO> getAvailableRooms(Integer campusId);
    RoomBookingResponseDTO bookRoom(RoomBookingRequestDTO request, String patronId);
    void cancelBooking(Integer bookingId, String patronId);
    void checkIn(Integer bookingId, String patronId);
    void checkOut(Integer bookingId, String patronId);
    void librarianCancel(Integer bookingId);
}
