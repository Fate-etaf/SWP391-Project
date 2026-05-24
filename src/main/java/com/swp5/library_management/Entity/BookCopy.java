package com.swp5.library_management.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "BookCopies", schema = "dbo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookCopy {

    @Id
    @Column(name = "CopyID", length = 30)
    private String copyId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "BookID", nullable = false)
    private Book book;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CampusID", nullable = false)
    private Campus campus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ShelfID")
    private Shelf shelf;

    @Column(name = "ConditionStatus", nullable = false, length = 20)
    private String conditionStatus;

    @Column(name = "CopyStatus", nullable = false, length = 20)
    private String copyStatus;

    @Column(name = "AcquiredAt")
    private LocalDateTime acquiredAt;
}
