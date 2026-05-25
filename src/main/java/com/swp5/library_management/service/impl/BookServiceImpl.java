package com.swp5.library_management.service.impl;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.swp5.library_management.dto.AddBookForm;
import com.swp5.library_management.entity.Author;
import com.swp5.library_management.entity.Book;
import com.swp5.library_management.repository.AuthorRepository;
import com.swp5.library_management.repository.BookRepository;
import com.swp5.library_management.service.BookService;

@Service
@Transactional(readOnly = true)
public class BookServiceImpl implements BookService {

    private final BookRepository bookRepository;
    private final AuthorRepository authorRepository;

    public BookServiceImpl(BookRepository bookRepository, AuthorRepository authorRepository) {
        this.bookRepository = bookRepository;
        this.authorRepository = authorRepository;
    }

    @Override
    public List<Book> getAllBooks() {
        return bookRepository.findAll();
    }

    @Override
    @Transactional
    public Book saveBook(AddBookForm form) {
        // ── Resolve author(s) ──────────────────────────────────────────────────
        Set<Author> authors = new HashSet<>();
        if (StringUtils.hasText(form.getAuthorName())) {
            // Support comma-separated names, e.g. "Alice, Bob"
            for (String name : form.getAuthorName().split(",")) {
                String trimmed = name.trim();
                if (!trimmed.isEmpty()) {
                    Author author = authorRepository
                            .findByAuthorNameIgnoreCase(trimmed)
                            .orElseGet(() -> authorRepository.save(
                                    Author.builder().authorName(trimmed).build()));
                    authors.add(author);
                }
            }
        }

        // ── Build Book entity ──────────────────────────────────────────────────
        Book book = new Book();
        book.setTitle(form.getTitle());
        book.setIsbn(form.getIsbn());
        book.setLanguage(form.getLanguage() != null ? form.getLanguage() : "Vietnamese");
        book.setPublishYear(form.getPublishYear());
        book.setCoverImageUrl(form.getCoverImageUrl());
        book.setDescription(form.getDescription());
        book.setCreatedAt(LocalDateTime.now());
        book.setAuthors(authors);

        return bookRepository.save(book);
    }
}
