package com.swp5.library_management.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "BorrowTicketDetails")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BorrowTicketDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "TicketDetailID")
    private Integer ticketDetailId;

    @ManyToOne
    @JoinColumn(name = "TicketID")
    private BorrowTicket borrowTicket;

    @ManyToOne
    @JoinColumn(name = "CopyID")
    private BookCopy bookCopy;

    @Column(name = "DueDate")
    private LocalDateTime dueDate;

    @Column(name = "ReturnDate")
    private LocalDateTime returnDate;

    @Column(name = "RenewalCount")
    private Integer renewalCount;

    @Column(name = "Status")
    private String status;

    @ManyToOne
    @JoinColumn(name = "ReturnCampusID")
    private Campus returnCampus;
}