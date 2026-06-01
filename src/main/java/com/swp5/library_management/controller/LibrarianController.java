package com.swp5.library_management.controller;

import com.swp5.library_management.dto.AddBookForm;
import com.swp5.library_management.service.BookService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

/**
 * Controller dành riêng cho Librarian (thủ thư).
 *
 * Tất cả các endpoint ở đây đều nằm dưới prefix /librarian,
 * tách biệt hoàn toàn khỏi BookController (public).
 *
 * Các route:
 *   GET  /librarian/books       → danh sách toàn bộ sách (quản lý)
 *   GET  /librarian/books/add   → form thêm sách mới
 *   POST /librarian/books/add   → xử lý lưu sách mới
 */
@Controller
@RequestMapping("/librarian")
public class LibrarianController {

    private final BookService bookService;

    public LibrarianController(BookService bookService) {
        this.bookService = bookService;
    }

    // ── Book List (Librarian view) ─────────────────────────────────────────────

    /**
     * GET /librarian/books → Hiển thị danh sách toàn bộ sách cho Librarian quản lý.
     */
    @GetMapping("/books")
    public String listBooks(Model model) {
        model.addAttribute("books", bookService.getAllBooks());
        return "books/list";
    }

    // ── Add Book ──────────────────────────────────────────────────────────────

    /**
     * GET /librarian/books/add → Hiển thị form thêm sách mới.
     */
    @GetMapping("/books/add")
    public String showAddBookForm(Model model) {
        model.addAttribute("bookForm", new AddBookForm());
        return "books/add";
    }

    /**
     * POST /librarian/books/add → Lưu sách mới và redirect về danh sách.
     */
    @PostMapping("/books/add")
    public String saveBook(@ModelAttribute("bookForm") AddBookForm form) {
        bookService.saveBook(form);
        return "redirect:/librarian/books";
    }
}
