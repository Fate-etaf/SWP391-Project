package com.swp5.library_management.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "TransferRequests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransferRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "TransferID")
    private Integer transferId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "RequestedBy", nullable = false)
    private User requestedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "FromCampusID", nullable = false)
    private Campus fromCampus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ToCampusID", nullable = false)
    private Campus toCampus;

    @Column(name = "RequestedAt")
    private LocalDateTime requestedAt;

    @Column(name = "ShippedAt")
    private LocalDateTime shippedAt;

    @Column(name = "ReceivedAt")
    private LocalDateTime receivedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ConfirmedBy")
    private User confirmedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ShippedBy")
    private User shippedBy;

    @Column(name = "Status", length = 50)
    private String status;

    @Column(name = "Note", columnDefinition = "NVARCHAR(MAX)")
    private String note;

    @OneToMany(mappedBy = "transferRequest", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TransferDetail> details;
}
