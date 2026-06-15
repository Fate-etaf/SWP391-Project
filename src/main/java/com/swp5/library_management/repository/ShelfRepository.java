package com.swp5.library_management.repository;

import com.swp5.library_management.entity.Shelf;
import org.springframework.data.jpa.repository.JpaRepository;



public interface ShelfRepository extends JpaRepository<Shelf, String> {
}