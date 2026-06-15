package com.swp5.library_management.admin.dto;

import java.time.LocalDateTime;

public class TransactionRecordDTO {
    private String bookTitle;
    private String copyId;
    private String patronName;
    private String librarianName;
    private LocalDateTime borrowDate;
    private LocalDateTime dueDate;
    private LocalDateTime returnDate;
    private String status;

    public TransactionRecordDTO(String bookTitle, String copyId, String patronName, String librarianName, 
                                LocalDateTime borrowDate, LocalDateTime dueDate, LocalDateTime returnDate, String status) {
        this.bookTitle = bookTitle;
        this.copyId = copyId;
        this.patronName = patronName;
        this.librarianName = librarianName;
        this.borrowDate = borrowDate;
        this.dueDate = dueDate;
        this.returnDate = returnDate;
        this.status = status;
    }

    // Getters
    public String getBookTitle() { return bookTitle; }
    public String getCopyId() { return copyId; }
    public String getPatronName() { return patronName; }
    public String getLibrarianName() { return librarianName; }
    public LocalDateTime getBorrowDate() { return borrowDate; }
    public LocalDateTime getDueDate() { return dueDate; }
    public LocalDateTime getReturnDate() { return returnDate; }
    public String getStatus() { return status; }
}