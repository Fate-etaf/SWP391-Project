package com.swp5.library_management.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.Set;

@Entity
@Table(name = "Majors", schema = "dbo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Major {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MajorID")
    private Integer majorId;

    @Column(name = "MajorCode", nullable = false, unique = true, length = 50)
    private String majorCode;

    @Column(name = "MajorName", nullable = false, length = 200)
    private String majorName;

    @ManyToMany
    @JoinTable(
        name = "MajorSubjects",
        joinColumns = @JoinColumn(name = "MajorID"),
        inverseJoinColumns = @JoinColumn(name = "SubjectCode")
    )
    private Set<Subject> subjects;
}
