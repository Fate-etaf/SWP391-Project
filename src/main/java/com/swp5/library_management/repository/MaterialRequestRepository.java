package com.swp5.library_management.repository;

import com.swp5.library_management.entity.MaterialRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MaterialRequestRepository extends JpaRepository<MaterialRequest, Integer> {
    List<MaterialRequest> findByPatronUserIdOrderByCreatedAtDesc(String patronId);
}
