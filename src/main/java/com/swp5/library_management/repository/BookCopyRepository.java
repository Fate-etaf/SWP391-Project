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
