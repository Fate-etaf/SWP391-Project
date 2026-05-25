package com.swp5.library_management.Entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "Users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @Column(name = "UserID", length = 20)
    private String userId; // Chính là Mã số / RollNumber / User ID

    @Column(name = "FullName", nullable = false, length = 150)
    private String fullName;

    @Column(name = "Email", nullable = false, unique = true, length = 150)
    private String email;

    @Column(name = "CampusID", nullable = false)
    private Integer campusId; // Mã cơ sở (1: Hà Nội, 2: Đà Nẵng...)

    @Column(name = "Status", nullable = false, length = 20)
    private String status = "Active";

    @Column(name = "BorrowingLocked", nullable = false)
    private Boolean borrowingLocked = false;

    @Column(name = "PasswordHash", nullable = false, length = 255)
    private String passwordHash; 

}