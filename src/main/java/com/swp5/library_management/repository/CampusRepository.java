package com.swp5.library_management.repository;

import com.swp5.library_management.entity.Campus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository cho Entity Campus.
 *
 * <p>Kế thừa JpaRepository đã cung cấp sẵn các phương thức cơ bản:
 * {@code findAll()}, {@code count()}, {@code findById()}, v.v.
 * Trang chủ chỉ cần 2 thứ: danh sách tên cơ sở (dropdown tìm kiếm)
 * và tổng số cơ sở (phần Metrics) — đều dùng được từ các method mặc định này.
 */
@Repository
public interface CampusRepository extends JpaRepository<Campus, Integer> {
    // Không cần khai báo thêm query — findAll() và count() từ JpaRepository là đủ.
}
