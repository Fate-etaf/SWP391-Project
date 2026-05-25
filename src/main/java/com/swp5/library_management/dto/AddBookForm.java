package com.swp5.library_management.dto;

/**
 * Form-backing DTO used by the Add Book page.
 * Uses simple String fields instead of JPA entity references
 * so Thymeleaf can bind them directly from the HTML form.
 */
public class AddBookForm {

    private String title;
    private String authorName;   // plain text – resolved to Author entity in service
    private String isbn;
    private String language = "Vietnamese";
    private Integer publishYear;
    private String coverImageUrl;
    private String description;

    // ── Getters & Setters ──────────────────────────────────────────────────────

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }

    public String getIsbn() { return isbn; }
    public void setIsbn(String isbn) { this.isbn = isbn; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public Integer getPublishYear() { return publishYear; }
    public void setPublishYear(Integer publishYear) { this.publishYear = publishYear; }

    public String getCoverImageUrl() { return coverImageUrl; }
    public void setCoverImageUrl(String coverImageUrl) { this.coverImageUrl = coverImageUrl; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
