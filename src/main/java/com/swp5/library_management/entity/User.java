package com.swp5.library_management.entity;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "Users",schema = "dbo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "RoleID")
    private Role role;
    
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "UserRoles",
        schema = "dbo",
        joinColumns = @JoinColumn(name = "UserID"),
        inverseJoinColumns = @JoinColumn(name = "RoleID")
    )
    @Builder.Default
    private Set<Role> roles = new HashSet<>();

    // Hàm tiện ích để Controller và HTML kiểm tra quyền 
    public boolean isLibrarian() {
        if (this.roles == null || this.roles.isEmpty()) return false;
        return this.roles.stream()
                .anyMatch(role -> "Librarian".equalsIgnoreCase(role.getRoleName()));
    }

    // Thêm thủ công Getter cho userId để IDE không báo lỗi
    public String getUserId() {
        return this.userId;
    }
}

