package com.swp5.library_management.repository;

import com.swp5.library_management.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
    
    Optional<User> findByEmail(String email);

    boolean existsByUserId(String userId);

    @Query("SELECT u FROM User u WHERE LOWER(u.userId) = LOWER(:userId)")
    Optional<User> findByUserIdIgnoreCase(@Param("userId") String userId);

    boolean existsByEmail(String email);

    /**
     * Đếm số User theo trạng thái.
     * Dùng trong HomeServiceImpl.getHomeStats() để lấy số "Bạn đọc tích cực".
     * Spring Data JPA tự sinh: SELECT COUNT(*) FROM Users WHERE Status = ?
     */
    long countByStatus(String status);
    
    Optional<User> findByUserIdAndEmailAndCampusId(String userId, String email, Integer campusId);
    Optional<User> findByUserIdAndCampusId(String userId, Integer campusId);
    
    @Query("SELECT u FROM User u WHERE u.userId = :identifier OR u.email = :identifier")
    Optional<User> findByIdentifier(@Param("identifier") String identifier);

    @Query(value = "SELECT r.RoleName FROM Roles r JOIN UserRoles ur ON r.RoleID = ur.RoleID WHERE ur.UserID = :userId", nativeQuery = true)
    List<String> findRolesByUserId(@Param("userId") String userId);

    @Query("SELECT DISTINCT u FROM User u LEFT JOIN u.roles r WHERE u.role.roleId = 3 OR u.role.roleName = 'Librarian' OR r.roleId = 3 OR r.roleName = 'Librarian'")
    List<User> findAnyLibrarian();

    // ════════════ CÁC HÀM PHỤC VỤ SEARCH / FILTER SINH VIÊN ════════════
    @Query("SELECT u FROM User u WHERE u.status = :status")
    List<User> findByStatus(@Param("status") String status);
    
    @Query("SELECT u FROM User u WHERE u.fullName LIKE %:search% OR u.userId LIKE %:search%")
    List<User> findByFullNameContainingOrUserIdContaining(@Param("search") String search);
    
    @Query("SELECT u FROM User u WHERE u.fullName LIKE %:search% AND u.status = :status")
    List<User> findByFullNameContainingAndStatus(@Param("search") String search, @Param("status") String status);
}