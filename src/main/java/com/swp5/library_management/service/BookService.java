package com.swp5.library_management.service;

import com.swp5.library_management.dto.AddBookForm;
import com.swp5.library_management.entity.Book;

import java.util.List;

public interface BookService {

    /**
     * Returns all books in the catalog.
     */
    List<Book> getAllBooks();

    /**
     * Saves a new book from the Add-Book form.
     * Resolves the author name to an existing Author row (or creates one).
     */
    Book saveBook(AddBookForm form);

}
