package com.swp5.library_management.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "BorrowTickets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BorrowTicket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "TicketID")
    private Integer ticketId;

    @ManyToOne
    @JoinColumn(name = "PatronID")
    private User patron;

    @ManyToOne
    @JoinColumn(name = "LibrarianID")
    private User librarian;

    @ManyToOne
    @JoinColumn(name = "CampusID")
    private Campus campus;

    @Column(name = "CreatedAt")
    private LocalDateTime createdAt;

    @Column(name = "Note")
    private String note;
}