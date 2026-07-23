package com.swp5.library_management.dto;

import lombok.*;

/**
 * DTO đại diện cho một thẻ sách trên trang kết quả tìm kiếm (UCG01).
 *
 * Lý do dùng DTO:
 *   - Tránh LazyInitializationException khi Thymeleaf render (collection lazy).
 *   - Chỉ expose đúng tập con dữ liệu cần thiết cho view tìm kiếm.
 *   - Trường coverColor (UI-only) không thuộc về entity.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookSearchResultDTO {

    private Integer bookId;

    private String title;

    /** ISBN-13 hoặc ISBN-10. */
    private String isbn;

    /** Tên tác giả ghép bởi dấu phẩy, VD: "Robert C. Martin, Kent Beck". */
    private String authorNames;

    /** Mã môn học liên kết, VD: "SWE201c". Null nếu sách chưa gắn môn. */
    private String subjectCode;

    /** Tên thể loại ghép bởi dấu phẩy. */
    private String categoryNames;

    /**
     * Số bản sao có trạng thái "Available".
     * Nếu người dùng chọn campus → đếm tại campus đó.
     * Nếu không chọn campus → đếm tất cả campus.
     */
    private long availableCount;

    /**
     * Tổng số bản sao (dùng để xác định "Đang bận" hay "Hết sách").
     */
    private long totalCount;

    /** URL ảnh bìa thật (từ DB). Null → dùng coverColor gradient. */
    private String coverImageUrl;

    /**
     * Chuỗi Tailwind gradient dùng khi không có ảnh bìa thật.
     * VD: "from-blue-700 to-indigo-900".
     * Gán xoay vòng trong Service.
     */
    private String coverColor;

    public String getCategoryName() {
        return this.categoryNames;
    }
}
