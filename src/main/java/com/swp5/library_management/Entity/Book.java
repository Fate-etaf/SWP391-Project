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

    // FK -> Subjects
    @ManyToOne
    @JoinColumn(name = "SubjectCode")
    private Subject subject;

    @Column(name = "ISBN", length = 20, unique = true)
    private String isbn;

    @Column(name = "Title", nullable = false, length = 300)
    private String title;

    // FK -> Publishers
    @ManyToOne
    @JoinColumn(name = "PublisherID")
    private Publisher publisher;

    @Column(name = "PublishYear")
    private Integer publishYear;

    @Column(name = "Edition", length = 50)
    private String edition;

    @Column(name = "Language")
    private String language;

    @Column(name = "Description", columnDefinition = "NVARCHAR(MAX)")
    private String description;

    @Column(name = "CoverImageURL", length = 500)
    private String coverImageUrl;

    @Column(name = "DefaultShelfCode", length = 50)
    private String defaultShelfCode;

    @Column(name = "CreatedAt")
    private java.time.LocalDateTime createdAt;

    @ManyToMany
    @JoinTable(
        name = "BookAuthors",
        joinColumns = @JoinColumn(name = "BookID"),
        inverseJoinColumns = @JoinColumn(name = "AuthorID")
    )
    private List<Author> authors;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = java.time.LocalDateTime.now();
        }
    }
}