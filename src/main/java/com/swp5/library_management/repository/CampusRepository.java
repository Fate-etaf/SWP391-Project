package com.swp5.library_management.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.swp5.library_management.entity.Campus;

@Repository
public interface CampusRepository extends JpaRepository<Campus, Integer> {
    Campus findByCampusName(String campusName);
}
