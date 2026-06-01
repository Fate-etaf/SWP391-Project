package com.swp5.library_management.controller;

import com.swp5.library_management.dto.AddBookForm;
import com.swp5.library_management.dto.BookDetailDTO;
import com.swp5.library_management.dto.BookSearchResultDTO;
import com.swp5.library_management.repository.CampusRepository;
import com.swp5.library_management.repository.SubjectRepository;
import com.swp5.library_management.service.BookService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.NoSuchElementException;

/**
 * Controller xử lý Use Case UCG01 (Search Books) và UCG02 (View Book Detail).
 * Các endpoint dành cho Librarian (Add Book) vẫn được giữ nguyên.
 */
@Controller
@RequestMapping("/books")
public class BookController {

    private final BookService      bookService;
    private final CampusRepository campusRepository;
    private final SubjectRepository subjectRepository;

    public BookController(BookService bookService,
                          CampusRepository campusRepository,
                          SubjectRepository subjectRepository) {
        this.bookService      = bookService;
        this.campusRepository = campusRepository;
        this.subjectRepository = subjectRepository;
    }

    // ── UCG01: Search Books ───────────────────────────────────────────────────

    /**
     * GET /books                → trang tìm kiếm (không có filter: hiện gợi ý)
     * GET /books?keyword=X      → kết quả tìm kiếm theo từ khoá
     * GET /books?subjectCode=X  → lọc theo mã môn (A1: Advanced search)
     * GET /books?campusId=X     → lọc theo campus
     */
    @GetMapping({"", "/"})
    public String searchBooks(
            @RequestParam(required = false) String  keyword,
            @RequestParam(required = false) String  subjectCode,
            @RequestParam(required = false) Integer campusId,
            Model model) {

        boolean hasSearch = StringUtils.hasText(keyword)
                         || StringUtils.hasText(subjectCode)
                         || campusId != null;

        if (hasSearch) {
            List<BookSearchResultDTO> results = bookService.searchBooks(keyword, subjectCode, campusId);
            model.addAttribute("results", results);
            model.addAttribute("noResults", results.isEmpty());

            // UCG01 – E1: Hiển thị sách mới nhất khi không tìm thấy kết quả
            if (results.isEmpty()) {
                model.addAttribute("recentBooks", bookService.getRecentBooks(campusId));
            }
        } else {
            // Chưa tìm kiếm → hiện gợi ý là sách mới nhất
            model.addAttribute("recentBooks", bookService.getRecentBooks(campusId));
        }

        // Populate dropdowns
        model.addAttribute("campuses",    campusRepository.findAll());
        model.addAttribute("subjects",    subjectRepository.findAll());

        // Giữ lại giá trị filter trên form sau khi submit
        model.addAttribute("keyword",            keyword);
        model.addAttribute("selectedCampusId",   campusId);
        model.addAttribute("selectedSubjectCode", subjectCode);
        model.addAttribute("hasSearch",          hasSearch);

        return "books/search";
    }

    // ── UCG02: View Book Detail ───────────────────────────────────────────────

    /**
     * GET /books/{id}            → chi tiết sách, tất cả campus
     * GET /books/{id}?campusId=X → chi tiết sách, lọc bản sao theo campus
     *
     * UCG02 – E1: Nếu bookId không tồn tại → redirect về trang tìm kiếm với thông báo.
     */
    @GetMapping("/{id}")
    public String viewBookDetail(
            @PathVariable Integer id,
            @RequestParam(required = false) Integer campusId,
            Model model) {

        try {
            BookDetailDTO book = bookService.getBookDetail(id, campusId);
            model.addAttribute("book",            book);
            model.addAttribute("campuses",        campusRepository.findAll());
            model.addAttribute("selectedCampusId", campusId);
            return "books/detail";

        } catch (NoSuchElementException e) {
            // UCG02 – E1: Sách không tồn tại hoặc đã bị xóa
            return "error/404";
        }
    }

    // library managment interface for librarian
    @GetMapping
    public String listBooks(Model model) {
        model.addAttribute("books", bookService.getAllBooks());
        return "books/list";
    }
    // ── Librarian: Add Book ───────────────────────────────────────────────────

    /** Show the Add Book form (dành cho Librarian). */
    @GetMapping("/add")
    public String showAddBookForm(Model model) {
        model.addAttribute("bookForm", new AddBookForm());
        return "books/add";
    }

    /** Handle Add Book form submission (dành cho Librarian). */
    @PostMapping("/add")
    public String saveBook(@ModelAttribute("bookForm") AddBookForm form) {
        bookService.saveBook(form);
        return "redirect:/books";
    }
}
