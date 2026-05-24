package com.swp5.library_management.Entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "Authors", schema = "dbo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Author {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "AuthorID")
    private Integer authorId;

    @Column(name = "AuthorName", nullable = false, length = 150)
    private String authorName;
}
