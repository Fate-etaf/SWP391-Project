package com.swp5.library_management.repository;

import com.swp5.library_management.entity.Category;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Integer> {

    /**
     * Lấy danh sách Category, sắp xếp theo ID tăng dần, có giới hạn số lượng.
     *
     * <p>Sử dụng {@link Pageable} để giới hạn kết quả thay vì {@code findTop5By...}
     * — cách này linh hoạt hơn vì Service có thể truyền vào bất kỳ giới hạn nào
     * (5, 10, v.v.) mà không cần thêm method mới vào Repository.
     *
     * <p>Ví dụ gọi trong Service:
     * {@code categoryRepository.findAllByOrderByCategoryIdAsc(PageRequest.of(0, 5))}
     */
    List<Category> findAllByOrderByCategoryIdAsc(Pageable pageable);
    Optional<Category> findByCategoryName(String categoryName);
}
