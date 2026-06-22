package com.swp5.library_management.repository;

import com.swp5.library_management.entity.BookCopy;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


public interface BookCopyRepository extends JpaRepository<BookCopy, String> {

    /**
     * Đếm số bản sao theo trạng thái.
     *
     * <p>Spring Data JPA tự sinh câu SQL:
     * {@code SELECT COUNT(*) FROM BookCopies WHERE CopyStatus = ?}
     *
     * <p>Dùng trong Service với tham số "Available" để lấy số bản sao sẵn sàng.
     * Sau này có thể mở rộng để đếm "Borrowed", "Damaged", v.v.
     */
    long countByCopyStatus(String copyStatus);

    long countByCampusCampusIdAndCopyStatus(Integer campusId, String copyStatus);

    long countByBookBookIdAndCampusCampusId(Integer bookId, Integer campusId);

    long countByBookBookIdAndCampusCampusIdAndCopyStatus(Integer bookId, Integer campusId, String copyStatus);

    List<BookCopy> findByCampusCampusId(Integer campusId);

    @Query("SELECT bc.copyStatus, COUNT(bc) FROM BookCopy bc WHERE bc.campus.campusId = :campusId GROUP BY bc.copyStatus")
    List<Object[]> countStatusGroupByCampus(@Param("campusId") Integer campusId);

    @Query("SELECT bc.campus.campusId, bc.copyStatus, COUNT(bc) FROM BookCopy bc GROUP BY bc.campus.campusId, bc.copyStatus")
    List<Object[]> countStatusGroupByCampusAll();

    @Query("SELECT bc.campus.campusId, bc.copyStatus, COUNT(bc) FROM BookCopy bc JOIN bc.book b JOIN b.categories cat " +
            "WHERE (:campusId IS NULL OR bc.campus.campusId = :campusId) " +
            "AND (:categoryId IS NULL OR cat.categoryId = :categoryId) " +
            "AND (:fromDate IS NULL OR bc.acquiredAt >= :fromDate) " +
            "AND (:toDate IS NULL OR bc.acquiredAt <= :toDate) " +
            "GROUP BY bc.campus.campusId, bc.copyStatus")
    List<Object[]> countStatusGroupByFilters(@Param("campusId") Integer campusId,
                                             @Param("categoryId") Integer categoryId,
                                             @Param("fromDate") LocalDateTime fromDate,
                                             @Param("toDate") LocalDateTime toDate);

    /**
     * UCR06 Bước 4: Tìm một bản sách vật lý sẵn sàng (Available) của đầu sách
     * tại cơ sở campus chỉ định. Trả về Optional để handle trường hợp hết sách.
     */
    Optional<BookCopy> findFirstByBookBookIdAndCampusCampusIdAndCopyStatus(
            Integer bookId, Integer campusId, String copyStatus);

    /**
     * UCG02: Lấy tất cả bản sao của một đầu sách không phân biệt campus.
     * Dùng khi người dùng chưa chọn campus filter.
     */
    List<BookCopy> findByBookBookId(Integer bookId);

    /**
     * UCG02: Đếm số bản sao Available theo từng bookId trong một batch.
     * Hiệu quả hơn N+1 khi tính availableCount cho trang tìm kiếm.
     * Trả về Object[]: [bookId (Integer), count (Long)]
     */
    @Query("""
        SELECT bc.book.bookId, COUNT(bc)
        FROM BookCopy bc
        WHERE bc.copyStatus = 'Available'
          AND bc.book.bookId IN :bookIds
        GROUP BY bc.book.bookId
    """)
    List<Object[]> countAvailableByBookIds(@Param("bookIds") List<Integer> bookIds);

    /**
     * UCG02: Đếm số bản sao Available theo từng bookId tại một campus cụ thể.
     * Trả về Object[]: [bookId (Integer), count (Long)]
     */
    @Query("""
        SELECT bc.book.bookId, COUNT(bc)
        FROM BookCopy bc
        WHERE bc.copyStatus = 'Available'
          AND bc.campus.campusId = :campusId
          AND bc.book.bookId IN :bookIds
        GROUP BY bc.book.bookId
    """)
    List<Object[]> countAvailableByBookIdsAndCampus(@Param("bookIds") List<Integer> bookIds,
                                                     @Param("campusId") Integer campusId);
}
