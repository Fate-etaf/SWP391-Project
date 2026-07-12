package com.swp5.library_management.controller;

import com.swp5.library_management.dto.BookDetailDTO;
import com.swp5.library_management.dto.BookSearchResultDTO;
import com.swp5.library_management.repository.CampusRepository;
import com.swp5.library_management.repository.SubjectRepository;
import com.swp5.library_management.service.BookService;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.swp5.library_management.security.CustomUserDetails;

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
    private final com.swp5.library_management.service.HomeService homeService;
    private final com.swp5.library_management.repository.CategoryRepository categoryRepository;
    private final com.swp5.library_management.repository.MajorRepository majorRepository;

    public BookController(BookService bookService,
                          CampusRepository campusRepository,
                          SubjectRepository subjectRepository,
                          com.swp5.library_management.service.HomeService homeService,
                          com.swp5.library_management.repository.CategoryRepository categoryRepository,
                          com.swp5.library_management.repository.MajorRepository majorRepository) {
        this.bookService      = bookService;
        this.campusRepository = campusRepository;
        this.subjectRepository = subjectRepository;
        this.homeService = homeService;
        this.categoryRepository = categoryRepository;
        this.majorRepository = majorRepository;
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
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) Integer majorId,
            @RequestParam(required = false) Integer campusId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "false") boolean ajax,
            Model model) {

        boolean hasSearch = StringUtils.hasText(keyword)
                         || StringUtils.hasText(subjectCode)
                         || categoryId != null
                         || majorId != null
                         || campusId != null;

        if (hasSearch) {
            Page<BookSearchResultDTO> resultPage = bookService.searchBooks(keyword, subjectCode, categoryId, majorId, campusId, page, 12);
            model.addAttribute("results", resultPage.getContent());
            model.addAttribute("totalPages", resultPage.getTotalPages());
            model.addAttribute("totalElements", resultPage.getTotalElements());
            model.addAttribute("currentPage", page);
            model.addAttribute("noResults", resultPage.isEmpty());
            
            // Nếu không có kết quả, hiển thị lại majorSections
            if (resultPage.isEmpty()) {
                model.addAttribute("majorSections", homeService.getMajorsWithRandomBooks());
            }
        } else {
            // Hiển thị gợi ý theo chuyên ngành
            model.addAttribute("majorSections", homeService.getMajorsWithRandomBooks());
        }

        // Populate dropdowns
        model.addAttribute("campuses",    campusRepository.findAll());
        model.addAttribute("subjects",    subjectRepository.findAll());
        model.addAttribute("categories",  categoryRepository.findAll());
        model.addAttribute("majors",      majorRepository.findAll());

        // Giữ lại giá trị filter trên form sau khi submit
        model.addAttribute("keyword",            keyword);
        model.addAttribute("selectedCampusId",   campusId);
        model.addAttribute("selectedSubjectCode", subjectCode);
        model.addAttribute("selectedCategoryId", categoryId);
        model.addAttribute("selectedMajorId",    majorId);
        model.addAttribute("hasSearch",          hasSearch);

        if (ajax) {
            return "books/search :: searchResults";
        }
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
            @RequestParam(required = false, defaultValue = "false") boolean ajax,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            jakarta.servlet.http.HttpSession session,
            Model model) {

        if (campusId == null) {
            if (userDetails != null && userDetails.getUser() != null && userDetails.getUser().getCampusId() != null) {
                campusId = userDetails.getUser().getCampusId();
            } else {
                Integer loggedInCampusId = (Integer) session.getAttribute("loggedInCampusId");
                if (loggedInCampusId != null) {
                    campusId = loggedInCampusId;
                }
            }
        } else if (campusId == 0) {
            campusId = null; // 0 means all campuses
        }

        try {
            BookDetailDTO book = bookService.getBookDetail(id, campusId);
            model.addAttribute("book",            book);
            model.addAttribute("campuses",        campusRepository.findAll());
            model.addAttribute("selectedCampusId", campusId);
            if (ajax) {
                return "books/detail :: copiesSection";
            }
            return "books/detail";

        } catch (NoSuchElementException e) {
            // UCG02 – E1: Sách không tồn tại hoặc đã bị xóa
            return "error/404";
        }
    }
}
