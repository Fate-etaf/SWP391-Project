package com.swp5.library_management.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;

@Entity
@Table(name = "RoomBookings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RoomBooking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "BookingID")
    private Integer bookingId;

    @ManyToOne
    @JoinColumn(name = "RoomID")
    private StudyRoom room;

    @ManyToOne
    @JoinColumn(name = "PatronID")
    private User patron;

    @Column(name = "BookingDate", nullable = false)
    private LocalDate bookingDate;

    @Column(name = "StartTime", nullable = false)
    private LocalTime startTime;

    @Column(name = "EndTime", nullable = false)
    private LocalTime endTime;

    @Column(name = "Purpose", length = 300)
    private String purpose;

    @Column(name = "ParticipantCount")
    private Integer participantCount;

    @Column(name = "Status", nullable = false, length = 20)
    private String status;

    @Lob
    @Column(name = "QRCode")
    private byte[] qrCode;

    @Column(name = "CreatedAt", nullable = false)
    private LocalDateTime createdAt;
}
