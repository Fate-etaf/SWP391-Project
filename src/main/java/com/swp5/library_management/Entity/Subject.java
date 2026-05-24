package com.swp5.library_management.Entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "Subjects", schema = "dbo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Subject {

    @Id
    @Column(name = "SubjectCode", length = 20)
    private String subjectCode;

    @Column(name = "SubjectName", nullable = false, length = 200)
    private String subjectName;

    @Column(name = "Description", length = 500)
    private String description;
}
