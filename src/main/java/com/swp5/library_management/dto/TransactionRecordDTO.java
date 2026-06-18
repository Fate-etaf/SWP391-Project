package com.swp5.library_management.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TransactionRecordDTO {
    private String bookTitle;
    private String copyId;
    private String patronId;         // New
    private String patronName;
    private String librarianName;
    private LocalDateTime borrowDate;
    private LocalDateTime dueDate;    // New
    private LocalDateTime returnDate;
    private String status;
    private int renewalCount;         // New
    private BigDecimal fineAmount;    // New

    public TransactionRecordDTO(String bookTitle, String copyId, String patronId, String patronName, 
                                String librarianName, LocalDateTime borrowDate, LocalDateTime dueDate, 
                                LocalDateTime returnDate, String status, int renewalCount, BigDecimal fineAmount) {
        this.bookTitle = bookTitle;
        this.copyId = copyId;
        this.patronId = patronId;
        this.patronName = patronName;
        this.librarianName = librarianName;
        this.borrowDate = borrowDate;
        this.dueDate = dueDate;
        this.returnDate = returnDate;
        this.status = status;
        this.renewalCount = renewalCount;
        this.fineAmount = fineAmount;
    }

    // --- GETTERS ---
    public String getBookTitle() { return bookTitle; }
    public String getCopyId() { return copyId; }
    public String getPatronId() { return patronId; }
    public String getPatronName() { return patronName; }
    public String getLibrarianName() { return librarianName; }
    public LocalDateTime getBorrowDate() { return borrowDate; }
    public LocalDateTime getDueDate() { return dueDate; }
    public LocalDateTime getReturnDate() { return returnDate; }
    public String getStatus() { return status; }
    public int getRenewalCount() { return renewalCount; }
    public BigDecimal getFineAmount() { return fineAmount; }
}