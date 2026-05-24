package com.swp5.library_management.Entity;

import java.util.List;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "Books", schema = "dbo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "BookID")
    private Integer bookId;

    @Column(name = "SubjectCode", nullable = false, length = 20)
    private String subjectCode;

    @Column(name = "ISBN", nullable = false, length = 20)
    private String isbn;

    @Column(name = "Title", nullable = false, length = 255)
    private String title;
    @ManyToMany
    @JoinTable(
        name = "BookAuthors",
        joinColumns = @JoinColumn(name = "BookID"),
        inverseJoinColumns = @JoinColumn(name = "AuthorID")
    )
    private List<Author> authors;
}