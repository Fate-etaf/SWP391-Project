package com.swp5.library_management.entity;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TransferDetailId implements Serializable {

    /*
     * Lý do có TransferDetail & TransferDetailId (Composite Key)
     * 
     * - Problem: CSDL dùng cả TransferID và CopyID làm khoá chính, nhưng Java/JPA
     * không hỗ trợ truyền trực tiếp 2 biến rời làm 1 khoá định danh.
     * 
     * - Giải quyết: Tách làm 2:
     * 1. TransferDetailId: Làm vai trò key (chỉ chứa dữ liệu ID thô).
     * 2. TransferDetail: Làm vai trò Value/Entity chính (chứa Object thật như
     * TransferRequest, BookCopy để truy vấn).
     * 
     * ==> Repository định danh được bản ghi, tránh lỗi nhập trùng dữ liệu
     * (VD: Không thể thêm 1 bản sao sách 2 lần vào cùng 1 phiếu).
     */

    @Column(name = "TransferID")
    private Integer transferId;

    @Column(name = "CopyID", length = 30)
    private String copyId;

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        TransferDetailId that = (TransferDetailId) o;
        return Objects.equals(transferId, that.transferId) &&
                Objects.equals(copyId, that.copyId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(transferId, copyId);
    }
}
