package com.swp5.library_management.service;

import com.swp5.library_management.dto.AddBookForm;
import com.swp5.library_management.dto.BookDetailDTO;
import com.swp5.library_management.dto.BookSearchResultDTO;
import com.swp5.library_management.entity.Book;
import org.springframework.data.domain.Page;
import java.util.List;
import java.util.NoSuchElementException;

public interface BookService {

    /**
     * Returns all books in the catalog (dành cho Librarian quản lý).
     */
    List<Book> getAllBooks();

    /**
     * Saves a new book from the Add-Book form.
     * Resolves the author name to an existing Author row (or creates one).
     */
    Book saveBook(AddBookForm form, Integer campusId);

    // ── UCG01 – Search Books ──────────────────────────────────────────────────

    /**
     * Tìm kiếm sách với các bộ lọc tuỳ chọn.
     * Tham số nào null/blank → bộ lọc đó bị bỏ qua (tìm tất cả).
     *
     * @param keyword     Từ khoá: tìm theo tiêu đề, tác giả, ISBN.
     * @param subjectCode Mã môn học để lọc (UCG01 – A1: Advanced search by Subject Code).
     * @param campusId    Campus để lọc số bản sao có sẵn. Null = tất cả campus.
     * @return Danh sách {@link BookSearchResultDTO} sắp xếp theo ngày nhập mới nhất.
     */
    Page<BookSearchResultDTO> searchBooks(String keyword, String subjectCode, Integer categoryId, Integer majorId, Integer campusId, int page, int size);

    /**
     * UCG01 – E1 Fallback: Lấy 8 cuốn sách mới nhất khi tìm không ra kết quả.
     *
     * @param campusId Campus để tính availableCount. Null = tất cả campus.
     */
    List<BookSearchResultDTO> getRecentBooks(Integer campusId);

    // ── UCG02 – View Book Detail ──────────────────────────────────────────────

    /**
     * Lấy thông tin chi tiết đầy đủ của một cuốn sách.
     *
     * @param bookId   ID của cuốn sách.
     * @param campusId Campus để lọc bảng bản sao. Null = hiển thị tất cả campus.
     * @return {@link BookDetailDTO} với danh sách bản sao (Available lên đầu).
     * @throws NoSuchElementException Nếu bookId không tồn tại → UCG02 E1.
     */
    BookDetailDTO getBookDetail(Integer bookId, Integer campusId);
}
