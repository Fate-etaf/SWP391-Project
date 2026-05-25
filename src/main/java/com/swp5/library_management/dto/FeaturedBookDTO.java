package com.swp5.library_management.dto;

import lombok.*;

/**
 * DTO đại diện cho một thẻ sách nổi bật (Featured Book Card) trên trang chủ.
 *
 * <p>Lý do dùng DTO thay vì truyền thẳng Entity Book ra View:
 * <ul>
 *   <li>View chỉ cần một tập con nhỏ của dữ liệu sách — không cần lộ
 *       toàn bộ quan hệ JPA phức tạp (copies, shelves, ...).</li>
 *   <li>Các trường phục vụ thuần UI như {@code coverColor} không thuộc về
 *       Entity — đặt trong DTO là đúng chỗ.</li>
 *   <li>Giảm nguy cơ lazy-loading gây lỗi "no session" trong Thymeleaf.</li>
 * </ul>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeaturedBookDTO {

    private String title;

    /** Chuỗi tên tác giả, ghép bởi dấu phẩy. VD: "Robert C. Martin" */
    private String authorNames;

    /** Mã môn học liên kết, VD: "SWE201c". Có thể là "N/A" nếu sách không gắn môn. */
    private String subjectCode;

    private String isbn;

    /** Tên thể loại chính của cuốn sách. */
    private String categoryName;

    /**
     * {@code true} nếu có ít nhất 1 bản sao trạng thái "Available".
     * Tên trường là {@code available} (không phải {@code isAvailable}) để
     * Thymeleaf có thể truy cập qua {@code book.available} (gọi getter {@code isAvailable()}).
     */
    private boolean available;

    /**
     * Chuỗi Tailwind CSS gradient dùng cho bìa sách mockup.
     * VD: "from-slate-700 to-slate-900"
     * Giá trị này được gán xoay vòng trong Service, không phải logic nghiệp vụ.
     */
    private String coverColor;
    
}
