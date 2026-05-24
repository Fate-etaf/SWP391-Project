package com.swp5.library_management.Entity;

import jakarta.persistence.*;
import lombok.*;

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
    private String userId;

    @Column(name = "FullName", nullable = false, length = 150)
    private String fullName;

    @Column(name = "Email", nullable = false, unique = true, length = 150)
    private String email;

    @Column(name = "PasswordHash", nullable = false, length = 255)
    private String passwordHash = "123";

    @Column(name = "Phone", length = 20)
    private String phone;

    @Column(name = "CampusID", nullable = false)
    private Integer campusId;

    @Column(name = "Status", nullable = false, length = 20)
    private String status = "Active";

    @Column(name = "BorrowingLocked", nullable = false)
    private Boolean borrowingLocked = false;
}
