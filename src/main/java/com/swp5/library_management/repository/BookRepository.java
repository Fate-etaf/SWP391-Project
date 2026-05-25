package com.swp5.library_management.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.swp5.library_management.entity.Book;

@Repository
public interface BookRepository extends JpaRepository<Book, Integer> {
    // findAll() from JpaRepository is sufficient for the book list page.
    // OSIV (Open Session In View) keeps the session open so Thymeleaf
    // can lazily initialize Authors, Categories, and Copies.
}
