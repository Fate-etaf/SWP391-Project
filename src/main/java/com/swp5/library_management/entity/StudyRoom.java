package com.swp5.library_management.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "StudyRooms", schema = "dbo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudyRoom {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "RoomID")
    private Integer roomId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CampusID", nullable = false)
    private Campus campus;

    @Column(name = "RoomName", nullable = false, length = 100)
    private String roomName;

    @Column(name = "Capacity", nullable = false)
    @Builder.Default
    private Integer capacity = 1;

    @Column(name = "Description", length = 500)
    private String description;

    @Column(name = "Status", nullable = false, length = 20)
    @Builder.Default
    private String status = "Available";
}
