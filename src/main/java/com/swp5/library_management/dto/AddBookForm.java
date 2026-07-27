package com.swp5.library_management.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Form-backing DTO used by the Add Book page.
 * Uses simple String/primitive fields so Thymeleaf can bind them directly
 * from HTML inputs — no JPA entity references needed in the form layer.
 */
public class AddBookForm {

    // ── Core fields ────────────────────────────────────────────────────────────
    @NotBlank(message = "Tiêu đề sách không được để trống")
    private String title;

    @NotBlank(message = "Tên tác giả không được để trống")
    private String authorName;      // comma-separated; resolved to Author entity(ies)

    @NotBlank(message = "ISBN không được để trống")
    private String isbn;

    @NotBlank(message = "Ngôn ngữ không được để trống")
    private String language = "Vietnamese";

    private Integer publishYear;
    private String coverImageUrl;
    private String description;

    // ── New librarian fields ───────────────────────────────────────────────────
    private String publisherName;   // resolved to Publisher entity (find or create)
    private String edition;         // e.g. "3rd Edition"
    private String shelfCode;      // must match an existing Shelf.shelfCode in the selected campus;
    private String subjectCode;     // must match an existing Subject.subjectCode
    private Integer copies;         // number of BookCopy records to create on save

    @NotNull(message = "Vui lòng chọn cơ sở (campus)")
    private Integer campusId;       // campus to create the copies in

    private String requestId;       // optional: the MaterialRequest that was auto-filled (used to mark it Available)

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

    public String getPublisherName() { return publisherName; }
    public void setPublisherName(String publisherName) { this.publisherName = publisherName; }

    public String getEdition() { return edition; }
    public void setEdition(String edition) { this.edition = edition; }

    public String getShelfCode() { return shelfCode; }
    public void setShelfCode(String shelfCode) { this.shelfCode = shelfCode; }

    public String getSubjectCode() { return subjectCode; }
    public void setSubjectCode(String subjectCode) { this.subjectCode = subjectCode; }

    public Integer getCopies() { return copies; }
    public void setCopies(Integer copies) { this.copies = copies; }

    public Integer getCampusId() { return campusId; }
    public void setCampusId(Integer campusId) { this.campusId = campusId; }

    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
}
