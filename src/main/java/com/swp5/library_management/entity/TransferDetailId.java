package com.swp5.library_management.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransferDetailId implements Serializable {

    @Column(name = "TransferID")
    private Integer transferId;

    @Column(name = "CopyID", length = 30)
    private String copyId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TransferDetailId that = (TransferDetailId) o;
        return Objects.equals(transferId, that.transferId) &&
               Objects.equals(copyId, that.copyId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(transferId, copyId);
    }
}
