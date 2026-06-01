package com.swp5.library_management.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

import com.swp5.library_management.entity.User;

@Repository
public interface UserRepository extends JpaRepository<User, String> {

    boolean existsByUserId(String userId);

    boolean existsByEmail(String email);

    /**
     * Đếm số User theo trạng thái.
     * Dùng trong HomeServiceImpl.getHomeStats() để lấy số "Bạn đọc tích cực".
     * Spring Data JPA tự sinh: SELECT COUNT(*) FROM Users WHERE Status = ?
     */
    long countByStatus(String status);
    
    // Tự động sinh câu lệnh SQL: SELECT * FROM Users WHERE UserID = ? AND Email = ? AND CampusID = ?
    Optional<User> findByUserIdAndEmailAndCampusId(String userId, String email, Integer campusId);
}