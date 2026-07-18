package com.swp5.library_management.repository;

import com.swp5.library_management.entity.Author;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;


public interface AuthorRepository extends JpaRepository<Author, Integer> {

    Optional<Author> findByAuthorNameIgnoreCase(String authorName);
}
