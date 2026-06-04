package com.swp5.library_management.controller;

import com.swp5.library_management.dto.AddBookForm;
import com.swp5.library_management.dto.BookDetailDTO;
import com.swp5.library_management.repository.CampusRepository;
import com.swp5.library_management.service.BookService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.NoSuchElementException;

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
public class ManageBookController {

    private final BookService      bookService;
    private final CampusRepository campusRepository;

    public ManageBookController(BookService bookService, CampusRepository campusRepository) {
        this.bookService      = bookService;
        this.campusRepository = campusRepository;
    }

    // ── Book List (Librarian view) ─────────────────────────────────────────────

    /**
     * GET /librarian/inventory → Hiển thị danh sách toàn bộ sách cho Librarian quản lý.
     */
    @GetMapping("/inventory")
    public String listBooks(Model model) {
        model.addAttribute("books", bookService.getAllBooks());
        return "inventory/list";
    }

    // ── Book Detail (Librarian view) ──────────────────────────────────────────

    /**
     * GET /librarian/inventory/{id} → Chi tiết sách: ảnh bìa, thông tin, bản sao + QR.
     */
    @GetMapping("/inventory/{id}")
    public String bookDetail(
            @PathVariable Integer id,
            @RequestParam(required = false) Integer campusId,
            Model model) {
        try {
            BookDetailDTO book = bookService.getBookDetail(id, campusId);
            model.addAttribute("book",             book);
            model.addAttribute("campuses",         campusRepository.findAll());
            model.addAttribute("selectedCampusId", campusId);
            return "inventory/detail";
        } catch (NoSuchElementException e) {
            return "error/404";
        }
    }

    // ── Add Book ──────────────────────────────────────────────────────────────

    /**
     * GET /librarian/inventory/add → Hiển thị form thêm sách mới.
     */
    @GetMapping("/inventory/add")
    public String showAddBookForm(Model model) {
        model.addAttribute("bookForm", new AddBookForm());
        return "inventory/add";
    }

    /**
     * POST /librarian/inventory/add → Lưu sách mới và redirect về danh sách.
     */
    @PostMapping("/inventory/add")
    public String saveBook(@ModelAttribute("bookForm") AddBookForm form, jakarta.servlet.http.HttpSession session) {
        Integer campusId = form.getCampusId();
        if (campusId == null) {
            campusId = (Integer) session.getAttribute("loggedInCampusId");
        }
        bookService.saveBook(form, campusId);
        return "redirect:/librarian/inventory";
    }
}
