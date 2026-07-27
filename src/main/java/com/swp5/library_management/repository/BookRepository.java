package com.swp5.library_management.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.swp5.library_management.entity.Book;


public interface BookRepository extends JpaRepository<Book, Integer> {

    /** Kiểm tra xem mã ISBN đã tồn tại trong hệ thống chưa. */
    boolean existsByIsbn(String isbn);

    /** Tìm sách theo ISBN (dùng khi import Excel để thêm copies vào sách đã có). */
    Optional<Book> findByIsbn(String isbn);

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
          AND (:categoryId IS NULL OR
               EXISTS (SELECT c FROM b.categories c WHERE c.categoryId = :categoryId))
          AND (:majorId IS NULL OR
               (:majorId > 0 AND EXISTS (SELECT 1 FROM Major m JOIN m.subjects ms WHERE m.majorId = :majorId AND ms.subjectCode = b.subject.subjectCode)) OR
               (:majorId = -1 AND (b.subject IS NULL OR NOT EXISTS (SELECT 1 FROM Major m2 JOIN m2.subjects ms2 WHERE ms2.subjectCode = b.subject.subjectCode)))
              )
          AND (:campusId IS NULL OR
               EXISTS (SELECT bc FROM BookCopy bc
                       WHERE bc.book = b AND bc.campus.campusId = :campusId))
    """)
    Page<Book> searchBooks(@Param("keyword")     String  keyword,
                           @Param("subjectCode") String  subjectCode,
                           @Param("categoryId")  Integer categoryId,
                           @Param("majorId")     Integer majorId,
                           @Param("campusId")    Integer campusId,
                           Pageable pageable);

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

    /**
     * Lấy 5 cuốn sách ngẫu nhiên theo chuyên ngành (Dùng NEWID() của SQL Server)
     */
    @Query(value = "SELECT TOP 5 b.* FROM Books b JOIN MajorSubjects ms ON b.SubjectCode = ms.SubjectCode WHERE ms.MajorID = :majorId ORDER BY NEWID()", nativeQuery = true)
    List<Book> findTop5RandomByMajor(@Param("majorId") Integer majorId);

    /**
     * Lấy 5 cuốn sách ngẫu nhiên không thuộc chuyên ngành nào (Dùng NEWID() của SQL Server)
     */
    @Query(value = "SELECT TOP 5 b.* FROM Books b WHERE b.SubjectCode IS NULL OR NOT EXISTS (SELECT 1 FROM MajorSubjects ms WHERE ms.SubjectCode = b.SubjectCode) ORDER BY NEWID()", nativeQuery = true)
    List<Book> findTop5RandomOutsideMajors();
}