package com.swp5.library_management.dto;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BorrowingHistoryDTO {
    private Integer ticketDetailId;
    private Integer ticketId;
    private Integer bookId;
    private String bookTitle;
    private String coverImageUrl;
    private String coverColor;
    private String authorNames;
    private String copyId;
    private LocalDateTime borrowDate;
    private LocalDateTime dueDate;
    private LocalDateTime returnDate;
    private Integer renewalCount;
    private String status; // "Borrowing", "Returned", "Overdue", "Lost", "Damaged", etc.
    private String returnCampusName;
}
