package com.swp5.library_management.service;

import java.util.List;

import com.swp5.library_management.entity.Book;

public interface BookService {

    /**
     * Returns all books in the catalog.
     */
    List<Book> getAllBooks();
}
