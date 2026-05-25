package com.swp5.library_management.repository;

import com.swp5.library_management.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
    
    // Tự động sinh câu lệnh SQL: SELECT * FROM Users WHERE UserID = ? AND Email = ? AND CampusID = ?
    Optional<User> findByUserIdAndEmailAndCampusId(String userId, String email, Integer campusId);
}