package com.swp5.library_management.dto;

import lombok.*;

import java.util.List;

/**
 * DTO đầy đủ cho trang Chi tiết sách (UCG02 – View Book Detail).
 *
 * Bao gồm:
 *   - Thông tin thư mục (bibliographic info): tiêu đề, ISBN, năm XB, edition, ngôn ngữ, mô tả...
 *   - Thông tin quan hệ được flatten: tên tác giả, nhà xuất bản, mã môn, thể loại.
 *   - Danh sách bản sao vật lý (có thể lọc theo campus).
 *   - Flag hasAvailableCopy để hiển thị/ẩn nút "Đặt giữ chỗ" (A1 trong UCG02).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookDetailDTO {

    private Integer bookId;

    private String title;

    private String isbn;

    private Integer publishYear;

    /** Phiên bản xuất bản, VD: "3rd Edition". Null nếu không có. */
    private String edition;

    /** Ngôn ngữ, VD: "English", "Vietnamese". */
    private String language;

    /** Mô tả / tóm tắt nội dung sách. */
    private String description;

    /** URL ảnh bìa (từ DB). Null → dùng coverColor gradient. */
    private String coverImageUrl;

    /**
     * Chuỗi Tailwind gradient dùng khi không có ảnh bìa thật.
     * VD: "from-slate-700 to-slate-900".
     */
    private String coverColor;

    /** Tên tác giả ghép bởi dấu phẩy. */
    private String authorNames;

    /** Tên nhà xuất bản. "Unknown" nếu chưa có. */
    private String publisherName;

    /** Mã môn học liên kết, VD: "SWE201c". Null nếu không gắn môn. */
    private String subjectCode;

    /** Tên môn học đầy đủ. */
    private String subjectName;

    /** Các thể loại sách, ghép bởi dấu phẩy. */
    private String categoryNames;

    /** Vị trí kệ của sách. */
    private String shelfCode;

    /** Số thứ tự kệ sách. */
    private Integer shelfNumber;

    /** Tên kệ sách. */
    private String shelfName;

    /** Chủ đề kệ sách. */
    private String shelfCodeTopic;

    /**
     * Danh sách bản sao vật lý. Nếu người dùng chọn campus → chỉ chứa bản sao
     * tại campus đó. Nếu không chọn campus → tất cả campus.
     * Sắp xếp: Available lên đầu.
     */
    private List<CopyRowDTO> copies;

    /**
     * {@code true} nếu trong danh sách {@code copies} có ít nhất 1 bản "Available".
     * Dùng để hiển thị/ẩn nút Reserve (UCG02 – A1).
     */
    private boolean hasAvailableCopy;
}
