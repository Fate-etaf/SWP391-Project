package com.swp5.library_management.Entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "Books")
public class Book {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "BookID")
    private Integer bookId;

    @Column(name = "SubjectCode", length = 20)
    private String subjectCode;

    @Column(name = "ISBN", length = 20)
    private String isbn;

    @Column(name = "Title", length = 300)
    private String title;

    @Column(name = "PublisherID")
    private Integer publisherId;

    @Column(name = "PublishYear")
    private Integer publishYear;

    @Column(name = "Edition", length = 50)
    private String edition;

    @Column(name = "Language", length = 50)
    private String language;

    @Column(name = "Description", columnDefinition = "nvarchar(max)")
    private String description;

    @Column(name = "CoverImageURL", length = 500)
    private String coverImageUrl;

    @Column(name = "DefaultShelfCode", length = 50)
    private String defaultShelfCode;

    @Column(name = "CreatedAt")
    private LocalDateTime createdAt;

    @ManyToMany(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
            name = "BookAuthors",
            joinColumns = @JoinColumn(name = "BookID"),
            inverseJoinColumns = @JoinColumn(name = "AuthorID")
    )
    private Set<Author> authors = new HashSet<>();

    public Book() {
    }

    public Book(String subjectCode, String isbn, String title, Integer publisherId, 
                Integer publishYear, String edition, String language, String description, 
                String coverImageUrl, String defaultShelfCode) {
        this.subjectCode = subjectCode;
        this.isbn = isbn;
        this.title = title;
        this.publisherId = publisherId;
        this.publishYear = publishYear;
        this.edition = edition;
        this.language = language;
        this.description = description;
        this.coverImageUrl = coverImageUrl;
        this.defaultShelfCode = defaultShelfCode;
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Integer getBookId() {
        return bookId;
    }

    public void setBookId(Integer bookId) {
        this.bookId = bookId;
    }

    public String getSubjectCode() {
        return subjectCode;
    }

    public void setSubjectCode(String subjectCode) {
        this.subjectCode = subjectCode;
    }

    public String getIsbn() {
        return isbn;
    }

    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Integer getPublisherId() {
        return publisherId;
    }

    public void setPublisherId(Integer publisherId) {
        this.publisherId = publisherId;
    }

    public Integer getPublishYear() {
        return publishYear;
    }

    public void setPublishYear(Integer publishYear) {
        this.publishYear = publishYear;
    }

    public String getEdition() {
        return edition;
    }

    public void setEdition(String edition) {
        this.edition = edition;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCoverImageUrl() {
        return coverImageUrl;
    }

    public void setCoverImageUrl(String coverImageUrl) {
        this.coverImageUrl = coverImageUrl;
    }

    public String getDefaultShelfCode() {
        return defaultShelfCode;
    }

    public void setDefaultShelfCode(String defaultShelfCode) {
        this.defaultShelfCode = defaultShelfCode;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Set<Author> getAuthors() {
        return authors;
    }

    public void setAuthors(Set<Author> authors) {
        this.authors = authors;
    }

    public void addAuthor(Author author) {
        this.authors.add(author);
        author.getBooks().add(this);
    }

    public void removeAuthor(Author author) {
        this.authors.remove(author);
        author.getBooks().remove(this);
    }
}
