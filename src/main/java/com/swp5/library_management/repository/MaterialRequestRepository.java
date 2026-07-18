package com.swp5.library_management.repository;

import com.swp5.library_management.entity.MaterialRequest;
import org.springframework.data.jpa.repository.JpaRepository;


import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MaterialRequestRepository extends JpaRepository<MaterialRequest, Integer> {
    List<MaterialRequest> findByPatronUserIdOrderByCreatedAtDesc(String patronId);

    long countByStatus(String status);

    long countByStatusAndPatronCampusId(String status, Integer campusId);

    @Query("SELECT m FROM MaterialRequest m WHERE " +
           "(:status IS NULL OR :status = '' OR m.status = :status) AND " +
           "(:search IS NULL OR :search = '' OR LOWER(m.title) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(m.author) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "ORDER BY m.createdAt DESC")
    List<MaterialRequest> findByStatusAndSearchTerm(@Param("status") String status, @Param("search") String search);

    @Query("SELECT m FROM MaterialRequest m WHERE " +
           "m.patron.campusId = :campusId AND " +
           "(:status IS NULL OR :status = '' OR m.status = :status) AND " +
           "(:search IS NULL OR :search = '' OR LOWER(m.title) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(m.author) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "ORDER BY m.createdAt DESC")
    List<MaterialRequest> findByStatusAndSearchTermAndCampusId(@Param("status") String status,
                                                               @Param("search") String search,
                                                               @Param("campusId") Integer campusId);
}
