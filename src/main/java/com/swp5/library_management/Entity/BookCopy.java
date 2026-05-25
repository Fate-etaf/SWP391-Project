package com.swp5.library_management.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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

    // Explicit getter for copyStatus (in case Lombok isn't processed)
    public String getCopyStatus() {
        return copyStatus;
    }

    public LocalDateTime getAcquiredAt() {
        return acquiredAt;
    }
}
