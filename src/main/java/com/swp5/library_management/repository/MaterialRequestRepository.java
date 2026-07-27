package com.swp5.library_management.repository;

import com.swp5.library_management.entity.MaterialRequest;
import org.springframework.data.jpa.repository.JpaRepository;


import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface MaterialRequestRepository extends JpaRepository<MaterialRequest, Integer> {
    
    @Modifying
    @Transactional
    @Query("UPDATE MaterialRequest m SET m.status = :status WHERE m.requestId = :id")
    void updateStatus(@Param("id") Integer id, @Param("status") String status);

    List<MaterialRequest> findByPatronUserIdOrderByCreatedAtDesc(String patronId);

    boolean existsByPatronUserIdAndTitleIgnoreCaseAndStatusNot(String patronId, String title, String status);

    /** Kiểm tra ISBN đã từng được request chưa (bất kỳ trạng thái nào). */
    boolean existsByIsbnIgnoreCase(String isbn);


    long countByStatus(String status);

    long countByStatusAndPatronCampusId(String status, Integer campusId);

    @Query("SELECT m FROM MaterialRequest m WHERE " +
           "(:status IS NULL OR :status = '' OR m.status = :status) AND " +
           "(:search IS NULL OR :search = '' OR LOWER(m.title) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(m.author) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
           "(:fromDate IS NULL OR m.createdAt >= :fromDate) AND " +
           "(:toDate IS NULL OR m.createdAt <= :toDate) " +
           "ORDER BY CASE WHEN m.status = 'Pending' THEN 0 ELSE 1 END ASC, m.createdAt DESC")
    List<MaterialRequest> findByStatusAndSearchTerm(
            @Param("status") String status,
            @Param("search") String search,
            @Param("fromDate") java.time.LocalDateTime fromDate,
            @Param("toDate") java.time.LocalDateTime toDate);

    @Query("SELECT m FROM MaterialRequest m WHERE " +
           "m.patron.campusId = :campusId AND " +
           "(:status IS NULL OR :status = '' OR m.status = :status) AND " +
           "(:search IS NULL OR :search = '' OR LOWER(m.title) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(m.author) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
           "(:fromDate IS NULL OR m.createdAt >= :fromDate) AND " +
           "(:toDate IS NULL OR m.createdAt <= :toDate) " +
           "ORDER BY CASE WHEN m.status = 'Pending' THEN 0 ELSE 1 END ASC, m.createdAt DESC")
    List<MaterialRequest> findByStatusAndSearchTermAndCampusId(
            @Param("status") String status,
            @Param("search") String search,
            @Param("campusId") Integer campusId,
            @Param("fromDate") java.time.LocalDateTime fromDate,
            @Param("toDate") java.time.LocalDateTime toDate);
}
