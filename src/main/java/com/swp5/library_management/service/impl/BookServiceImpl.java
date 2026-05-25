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
import com.swp5.library_management.entity.Publisher;
import com.swp5.library_management.entity.Subject;
import com.swp5.library_management.repository.AuthorRepository;
import com.swp5.library_management.repository.BookRepository;
import com.swp5.library_management.repository.PublisherRepository;
import com.swp5.library_management.repository.SubjectRepository;
import com.swp5.library_management.service.BookService;

@Service
@Transactional(readOnly = true)
public class BookServiceImpl implements BookService {

    private final BookRepository    bookRepository;
    private final AuthorRepository  authorRepository;
    private final PublisherRepository publisherRepository;
    private final SubjectRepository   subjectRepository;

    public BookServiceImpl(BookRepository bookRepository,
                           AuthorRepository authorRepository,
                           PublisherRepository publisherRepository,
                           SubjectRepository subjectRepository) {
        this.bookRepository    = bookRepository;
        this.authorRepository  = authorRepository;
        this.publisherRepository = publisherRepository;
        this.subjectRepository   = subjectRepository;
    }

    @Override
    public List<Book> getAllBooks() {
        return bookRepository.findAll();
    }

    @Override
    @Transactional
    public Book saveBook(AddBookForm form) {

        // ── 1. Resolve Author(s) — find or create ──────────────────────────────
        Set<Author> authors = new HashSet<>();
        if (StringUtils.hasText(form.getAuthorName())) {
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

        // ── 2. Resolve Publisher — find or create ──────────────────────────────
        Publisher publisher = null;
        if (StringUtils.hasText(form.getPublisherName())) {
            publisher = publisherRepository
                    .findByPublisherNameIgnoreCase(form.getPublisherName().trim())
                    .orElseGet(() -> publisherRepository.save(
                            Publisher.builder()
                                    .publisherName(form.getPublisherName().trim())
                                    .build()));
        }

        // ── 3. Resolve Subject — lookup only (must exist in DB) ───────────────
        Subject subject = null;
        if (StringUtils.hasText(form.getSubjectCode())) {
            subject = subjectRepository
                    .findById(form.getSubjectCode().trim())
                    .orElse(null);   // silently ignore unknown codes
        }

        // ── 4. Build and save the Book ─────────────────────────────────────────
        Book book = new Book();
        book.setTitle(form.getTitle());
        book.setIsbn(form.getIsbn());
        book.setLanguage(StringUtils.hasText(form.getLanguage()) ? form.getLanguage() : "Vietnamese");
        book.setPublishYear(form.getPublishYear());
        book.setCoverImageUrl(form.getCoverImageUrl());
        book.setDescription(form.getDescription());
        book.setEdition(form.getEdition());
        book.setDefaultShelfCode(form.getDefaultShelfCode());
        book.setCreatedAt(LocalDateTime.now());
        book.setAuthors(authors);
        book.setPublisher(publisher);
        book.setSubject(subject);

        return bookRepository.save(book);
    }
}

