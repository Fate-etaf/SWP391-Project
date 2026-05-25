package com.swp5.library_management.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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

    // Explicit getters (safe if Lombok isn't available at compile time)
    public Integer getCampusId() {
        return campusId;
    }

    public String getCampusName() {
        return campusName;
    }
}
