package com.swp5.library_management.repository;

import com.swp5.library_management.entity.MaterialRequest;
import org.springframework.data.jpa.repository.JpaRepository;


import java.util.List;

public interface MaterialRequestRepository extends JpaRepository<MaterialRequest, Integer> {
    List<MaterialRequest> findByPatronUserIdOrderByCreatedAtDesc(String patronId);
}
