package com.swp5.library_management.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.swp5.library_management.entity.Major;

@Repository
public interface MajorRepository extends JpaRepository<Major, Integer> {
}
