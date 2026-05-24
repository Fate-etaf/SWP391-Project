package com.swp5.library_management.service;

import com.swp5.library_management.Entity.Book;

import java.util.List;

public interface BookService {

    /**
     * Returns all books in the catalog.
     */
    List<Book> getAllBooks();

    /**
     * Saves a new or existing book.
     */
    Book saveBook(Book book);
}
