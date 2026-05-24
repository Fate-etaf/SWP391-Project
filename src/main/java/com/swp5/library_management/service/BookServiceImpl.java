package com.swp5.library_management.service;

import com.swp5.library_management.Entity.Book;
import com.swp5.library_management.repository.BookRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class BookServiceImpl implements BookService {

    private final BookRepository bookRepository;

    public BookServiceImpl(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    @Override
    public List<Book> getAllBooks() {
        return bookRepository.findAll();
    }

    @Override
    @Transactional
    public Book saveBook(Book book) {
        if (book.getCreatedAt() == null) {
            book.setCreatedAt(java.time.LocalDateTime.now());
        }
        return bookRepository.save(book);
    }
}
