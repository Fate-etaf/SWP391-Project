package com.swp5.library_management.dto;

import lombok.*;

/**
 * DTO một hàng trong bảng phân bổ bản sao tại trang Chi tiết sách (UCG02).
 *
 * Mỗi CopyRowDTO tương ứng với 1 bản sao vật lý (1 row trong bảng BookCopies).
 * Dữ liệu đã được "phẳng hóa" (flattened) từ quan hệ BookCopy → Campus → Shelf.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CopyRowDTO {

    /** Barcode / mã định danh bản sao vật lý, VD: "BK-HN-00123". */
    private String copyId;

    /** Tên cơ sở đang giữ bản sao này, VD: "Hà Nội (Hòa Lạc)". */
    private String campusName;

    /** Tình trạng vật lý: Good / Damaged / Lost. */
    private String conditionStatus;

    /** Trạng thái mượn: Available / Borrowed / Reserved / Maintenance. */
    private String copyStatus;

    /** Mã ký hiệu phân loại kệ, VD: "QA76.9". Hiển thị "—" nếu chưa xếp kệ. */
    private String shelfCode;

    /** Tên / mô tả kệ, VD: "Khoa học máy tính". Hiển thị "—" nếu chưa xếp kệ. */
    private String shelfName;
}
