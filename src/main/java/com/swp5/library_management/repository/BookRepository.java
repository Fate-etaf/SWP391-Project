package com.swp5.library_management.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.swp5.library_management.entity.Book;

@Repository
public interface BookRepository extends JpaRepository<Book, Integer> {

    /**
     * Tìm kiếm sách theo tiêu đề HOẶC tên tác giả (không phân biệt hoa thường).
     *
     * FIX: Phiên bản cũ dùng derived query "findBy...AuthorNamesContaining..." sẽ crash
     * vì "authorNames" là computed method, không phải @Column JPA field.
     * Phải dùng @Query với JPQL JOIN vào bảng quan hệ authors.authorName.
     *
     * Dùng DISTINCT để tránh trả về book trùng lặp khi có nhiều tác giả khớp.
     */
    @Query("""
        SELECT DISTINCT b FROM Book b
        LEFT JOIN b.authors a
        WHERE LOWER(b.title)    LIKE LOWER(CONCAT('%', :keyword, '%'))
           OR LOWER(a.authorName) LIKE LOWER(CONCAT('%', :keyword, '%'))
    """)
    List<Book> searchByTitleOrAuthor(@Param("keyword") String keyword);

    /**
     * Lấy 4 sách mới nhập kho nhất, sắp xếp theo createdAt giảm dần.
     *
     * @EntityGraph tải eagerly authors/categories/copies/subject trong 1 câu SQL,
     * tránh N+1 queries và LazyInitializationException trong Thymeleaf.
     */
    @EntityGraph(attributePaths = {"authors", "categories", "copies", "subject"})
    List<Book> findTop4ByOrderByCreatedAtDesc();

    /**
     * Đếm số sách theo từng thể loại bằng 1 câu query duy nhất.
     * Trả về List<Object[]>: [CategoryID (Integer), count (Long)].
     * COUNT(DISTINCT b) đảm bảo mỗi Book chỉ được đếm 1 lần.
     */
    @Query("SELECT c.categoryId, COUNT(DISTINCT b) FROM Book b JOIN b.categories c GROUP BY c.categoryId")
    List<Object[]> countBooksGroupedByCategory();

    // ────────────────────────────────────────────────────────────────────────
    // UCG01 – Search Books
    // ────────────────────────────────────────────────────────────────────────

    /**
     * Tìm kiếm sách với các bộ lọc tuỳ chọn:
     *   - keyword  : tìm theo tiêu đề, tên tác giả, hoặc ISBN (case-insensitive).
     *   - subjectCode: lọc theo mã môn học liên kết.
     *   - campusId : chỉ trả về sách có ít nhất 1 bản sao tại campus đó.
     *
     * Tham số nào để null/blank → bộ lọc đó bị bỏ qua.
     * EXISTS thay vì JOIN để tránh duplicate rows khi 1 sách có nhiều bản sao.
     */
    @Query("""
        SELECT DISTINCT b FROM Book b
        LEFT JOIN b.authors a
        WHERE (:keyword IS NULL OR :keyword = '' OR
               LOWER(b.title) LIKE LOWER(CONCAT('%',:keyword,'%')) OR
               LOWER(a.authorName) LIKE LOWER(CONCAT('%',:keyword,'%')) OR
               b.isbn LIKE CONCAT('%',:keyword,'%'))
          AND (:subjectCode IS NULL OR :subjectCode = '' OR
               b.subject.subjectCode = :subjectCode)
          AND (:campusId IS NULL OR
               EXISTS (SELECT bc FROM BookCopy bc
                       WHERE bc.book = b AND bc.campus.campusId = :campusId))
        ORDER BY b.createdAt DESC
    """)
    List<Book> searchBooks(@Param("keyword")     String  keyword,
                           @Param("subjectCode") String  subjectCode,
                           @Param("campusId")    Integer campusId);

    // ────────────────────────────────────────────────────────────────────────
    // UCG02 – View Book Detail
    // ────────────────────────────────────────────────────────────────────────

    /**
     * Load đầy đủ quan hệ của 1 cuốn sách bằng EntityGraph để tránh N+1
     * và LazyInitializationException khi ánh xạ sang BookDetailDTO.
     *
     * Override findById từ JpaRepository — Spring Data JPA áp dụng EntityGraph
     * như một JPA hint trên query tìm theo PK.
     */
    @Override
    @EntityGraph(attributePaths = {"authors", "categories", "copies",
                                   "copies.campus", "copies.shelf",
                                   "subject", "publisher"})
    Optional<Book> findById(Integer bookId);

    /**
     * UCG01 – E1 Fallback: Lấy 8 sách mới nhất khi không có kết quả tìm kiếm.
     * Authors + categories + copies được load eager để map sang DTO không bị lỗi lazy.
     */
    @EntityGraph(attributePaths = {"authors", "categories", "copies", "subject"})
    List<Book> findTop8ByOrderByCreatedAtDesc();
}