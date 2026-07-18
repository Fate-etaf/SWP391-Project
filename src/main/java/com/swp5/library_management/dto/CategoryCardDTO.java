package com.swp5.library_management.dto;

import lombok.*;

/**
 * DTO đại diện cho một thẻ thể loại (Category Card) hiển thị trên trang chủ.
 *
 * <p>Tương tự {@link FeaturedBookDTO}, trường {@code bgClass} là thông tin
 * thuần UI (CSS Tailwind) — không thuộc Entity {@code Category} và không
 * nên lưu vào DB. Service sẽ gán xoay vòng theo index.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryCardDTO {

    /** Tên thể loại, VD: "Công nghệ thông tin" */
    private String name;

    /** Số sách đã định dạng đẹp, VD: "1,240 đầu sách" */
    private String bookCount;

    /**
     * CSS class Tailwind cho icon box, đã gồm màu nền và màu chữ.
     * VD: "bg-blue-50 text-blue-600"
     * Thymeleaf dùng trực tiếp: {@code th:classappend="${cat.bgClass}"}
     * — thay thế toàn bộ chuỗi if-else lồng nhau trong template cũ.
     */
    private String bgClass;

    /**
     * Color key ngắn gọn (tuỳ chọn) dùng khi cần điều kiện CSS phức hơn.
     * VD: "blue", "green", "amber", "indigo", "rose"
     */
    private String colorKey;
}
