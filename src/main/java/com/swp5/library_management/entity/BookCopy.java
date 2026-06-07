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
    @JoinColumn(name = "ShelfCode", referencedColumnName = "ShelfCode")
    private Shelf shelf;

    @Column(name = "ConditionStatus", nullable = false, length = 20)
    private String conditionStatus;

    @Column(name = "CopyStatus", nullable = false, length = 20)
    private String copyStatus;

    @Column(name = "AcquiredAt")
    private LocalDateTime acquiredAt;

    /**
     * Stores the value used to generate the QR code for this physical copy.
     * Value = copyId (set automatically on first persist via @PostPersist).
     * The actual QR image is rendered on demand by QrCodeService.
     */
    @Column(name = "QRCode", length = 30)
    private String qrCode;

    // ── JPA lifecycle ─────────────────────────────────────────────────────────

    /**
     * After the row is inserted for the first time, ensure qrCode == copyId.
     * If the DB already set it, this is a no-op (same value).
     */
    @PostPersist
    void initQrCode() {
        if (this.qrCode == null) {
            this.qrCode = this.copyId;
        }
    }

    // ── Explicit getters (keep for safety alongside Lombok) ───────────────────

    public String getCopyStatus() {
        return copyStatus;
    }

    public LocalDateTime getAcquiredAt() {
        return acquiredAt;
    }
}

