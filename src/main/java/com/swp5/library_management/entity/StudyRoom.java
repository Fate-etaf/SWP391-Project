package com.swp5.library_management.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "StudyRooms")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StudyRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "RoomID")
    private Integer roomId;

    @ManyToOne
    @JoinColumn(name = "CampusID")
    private Campus campus;

    @Column(name = "RoomName", nullable = false, length = 100)
    private String roomName;

    @Column(name = "Capacity", nullable = false)
    private Integer capacity;

    @Column(name = "Description", length = 500)
    private String description;

    @Column(name = "Status", nullable = false, length = 20)
    private String status;
}
