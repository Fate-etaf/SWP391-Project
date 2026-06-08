package com.swp5.library_management.entity;
import jakarta.persistence.*;
import lombok.*;
@Entity
@Table(name = "Roles", schema = "dbo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Role {

    @Id
    @Column(name = "RoleID")
    private Integer roleId;

    @Column(name = "RoleName")
    private String roleName;
}
