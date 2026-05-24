package com.swp5.library_management.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SubjectCode", referencedColumnName = "SubjectCode")
    private Subject subject;

    @Column(name = "ISBN", unique = true, length = 20)
    private String isbn;

    @Column(name = "Title", nullable = false, length = 300)
    private String title;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PublisherID")
    private Publisher publisher;

    @Column(name = "PublishYear")
    private Integer publishYear;

    @Column(name = "Edition", length = 50)
    private String edition;

    @Column(name = "Language", nullable = false, length = 50)
    private String language;

    @Column(name = "Description", columnDefinition = "NVARCHAR(MAX)")
    private String description;

    @Column(name = "CoverImageURL", length = 500)
    private String coverImageUrl;

    @Column(name = "DefaultShelfCode", length = 50)
    private String defaultShelfCode;

    @Column(name = "CreatedAt", nullable = false)
    private LocalDateTime createdAt;

    // ── Many-to-Many: Authors ────────────────────────────────────────────────
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "BookAuthors",
            schema = "dbo",
            joinColumns = @JoinColumn(name = "BookID"),
            inverseJoinColumns = @JoinColumn(name = "AuthorID")
    )
    @Builder.Default
    private Set<Author> authors = new HashSet<>();

    // ── Many-to-Many: Categories ─────────────────────────────────────────────
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "BookCategories",
            schema = "dbo",
            joinColumns = @JoinColumn(name = "BookID"),
            inverseJoinColumns = @JoinColumn(name = "CategoryID")
    )
    @Builder.Default
    private Set<Category> categories = new HashSet<>();

    // ── One-to-Many: Copies ──────────────────────────────────────────────────
    @OneToMany(mappedBy = "book", fetch = FetchType.LAZY)
    @Builder.Default
    private Set<BookCopy> copies = new HashSet<>();

    // ── Computed helper ──────────────────────────────────────────────────────
    /** Returns the number of copies whose status is 'Available'. */
    public long getAvailableCount() {
        if (copies == null || copies.isEmpty()) return 0;
        return copies.stream()
                .filter(c -> "Available".equals(c.getCopyStatus()))
                .count();
    }

    /** Returns comma-separated author names for display. */
    public String getAuthorNames() {
        if (authors == null || authors.isEmpty()) return "Unknown Author";
        return authors.stream()
                .map(Author::getAuthorName)
                .collect(Collectors.joining(", "));
    }
}