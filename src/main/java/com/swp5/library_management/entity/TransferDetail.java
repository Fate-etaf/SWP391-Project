package com.swp5.library_management.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "TransferDetails")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransferDetail {

    @EmbeddedId
    private TransferDetailId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("transferId")
    @JoinColumn(name = "TransferID", nullable = false)
    private TransferRequest transferRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("copyId")
    @JoinColumn(name = "CopyID", nullable = false)
    private BookCopy copy;
}
