package com.swp5.library_management.repository;

import com.swp5.library_management.entity.BookCopy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
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
}
package com.swp5.library_management.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.swp5.library_management.entity.BookCopy;

@Repository
public interface BookCopyRepository extends JpaRepository<BookCopy, String> {

    long countByCampusCampusIdAndCopyStatus(Integer campusId, String copyStatus);

    long countByCopyStatus(String copyStatus);

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
}
