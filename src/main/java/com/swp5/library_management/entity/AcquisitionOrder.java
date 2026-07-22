package com.swp5.library_management.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "AcquisitionOrders", schema = "dbo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AcquisitionOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "OrderID")
    private Integer orderId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "LibrarianID", nullable = false)
    private User librarian;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CampusID", nullable = false)
    private Campus campus;

    @Column(name = "SupplierName", length = 200)
    private String supplierName;

    @Column(name = "ReceivedDate", nullable = false)
    private LocalDateTime receivedDate;

    @Column(name = "Note", length = 500)
    private String note;

    /**
     * Allowed values: 'Pending', 'Received', 'Cancelled'
     */
    @Column(name = "Status", nullable = false, length = 20)
    private String status;

    // ── One-to-Many: Order Details ───────────────────────────────────────────
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<AcquisitionOrderDetail> orderDetails = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (receivedDate == null) {
            receivedDate = LocalDateTime.now();
        }
        if (status == null) {
            status = "Pending";
        }
    }
}
