package com.swp5.library_management.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "AcquisitionOrderDetails", schema = "dbo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AcquisitionOrderDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "OrderDetailID")
    private Integer orderDetailId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "OrderID", nullable = false)
    private AcquisitionOrder order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "BookID", nullable = false)
    private Book book;

    @Column(name = "QuantityOrdered", nullable = false)
    private Integer quantityOrdered;

    @Column(name = "QuantityReceived", nullable = false)
    private Integer quantityReceived;

    @Column(name = "UnitPrice", precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @PrePersist
    public void prePersist() {
        if (quantityReceived == null) {
            quantityReceived = 0;
        }
    }
}
