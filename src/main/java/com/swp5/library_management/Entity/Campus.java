package com.swp5.library_management.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "Campuses", schema = "dbo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Campus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "CampusID")
    private Integer campusId;

    @Column(name = "CampusName", nullable = false, length = 100)
    private String campusName;

    @Column(name = "Address", nullable = false, length = 255)
    private String address;

    @Column(name = "Phone", length = 20)
    private String phone;
}
