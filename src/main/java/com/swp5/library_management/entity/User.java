package com.swp5.library_management.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "Users",schema = "dbo")
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
    @Builder.Default
    private String passwordHash = "123";

    @Column(name = "Phone", length = 20)
    @Builder.Default
    private String phone = "";

    @Column(name = "CampusID", nullable = false)
    private Integer campusId;

    @Column(name = "Status", nullable = false, length = 20)
    @Builder.Default
    private String status = "Active";

    @Column(name = "BorrowingLocked", nullable = false)
    @Builder.Default
    private Boolean borrowingLocked = false;
}

