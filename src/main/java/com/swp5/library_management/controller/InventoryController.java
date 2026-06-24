package com.swp5.library_management.controller;

import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.swp5.library_management.dto.AddBookForm;
import com.swp5.library_management.dto.BookDetailDTO;
import com.swp5.library_management.dto.BookSearchResultDTO;
import com.swp5.library_management.dto.InventoryOverviewDTO;
import com.swp5.library_management.entity.Campus;
import com.swp5.library_management.entity.Category;
import com.swp5.library_management.repository.CampusRepository;
import com.swp5.library_management.repository.CategoryRepository;
import com.swp5.library_management.repository.MajorRepository;
import com.swp5.library_management.repository.ShelfRepository;
import com.swp5.library_management.repository.SubjectRepository;
import com.swp5.library_management.service.BookService;
import com.swp5.library_management.service.HomeService;
import com.swp5.library_management.service.InventoryService;

@Controller
@RequestMapping("/librarian")
public class InventoryController {

    private final InventoryService inventoryService;
    private final CampusRepository campusRepository;
    private final CategoryRepository categoryRepository;
    private final ShelfRepository shelfRepository;
    private final BookService      bookService;
    private final SubjectRepository subjectRepository;
    private final MajorRepository majorRepository;
    private final HomeService homeService;

    public InventoryController(InventoryService inventoryService,
                               CampusRepository campusRepository,
                               CategoryRepository categoryRepository,
                               ShelfRepository shelfRepository,
                               BookService bookService,
                               SubjectRepository subjectRepository,
                               MajorRepository majorRepository,
                               HomeService homeService) {
        this.inventoryService = inventoryService;
        this.campusRepository = campusRepository;
        this.categoryRepository = categoryRepository;
        this.shelfRepository = shelfRepository;
        this.bookService = bookService;
        this.subjectRepository = subjectRepository;
        this.majorRepository = majorRepository;
        this.homeService = homeService;
    }

    @GetMapping("/inventory/list")
    public String listBooks(
            @RequestParam(required = false) String  keyword,
            @RequestParam(required = false) String  subjectCode,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) Integer majorId,
            @RequestParam(required = false) Integer campusId,
            @RequestParam(defaultValue = "0") int page,
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
            
            if (resultPage.isEmpty()) {
                model.addAttribute("majorSections", homeService.getMajorsWithRandomBooks());
            }
        } else {
            model.addAttribute("majorSections", homeService.getMajorsWithRandomBooks());
        }

        model.addAttribute("campuses",    campusRepository.findAll());
        model.addAttribute("subjects",    subjectRepository.findAll());
        model.addAttribute("categories",  categoryRepository.findAll());
        model.addAttribute("majors",      majorRepository.findAll());

        model.addAttribute("keyword",            keyword);
        model.addAttribute("selectedCampusId",   campusId);
        model.addAttribute("selectedSubjectCode", subjectCode);
        model.addAttribute("selectedCategoryId", categoryId);
        model.addAttribute("selectedMajorId",    majorId);
        model.addAttribute("hasSearch",          hasSearch);
        model.addAttribute("searchAction",       "/librarian/inventory/list");

        return "inventory/list";
    }

     @GetMapping("/inventory/add")
    public String showAddBookForm(Model model) {
        model.addAttribute("bookForm", new AddBookForm());
        model.addAttribute("shelves", shelfRepository.findAll());
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
    @GetMapping("/inventory/{id}")
    public String bookDetail(
            @PathVariable Integer id,
            @RequestParam(required = false) Integer campusId,
            Model model) {
        try {
            BookDetailDTO book = bookService.getBookDetail(id, campusId);
            System.out.println("DTO copies size = " + book.getCopies().size());
            model.addAttribute("book", book);
            model.addAttribute("campuses",         campusRepository.findAll());
            model.addAttribute("selectedCampusId", campusId);
            return "inventory/detail";
        } catch (NoSuchElementException e) {
            return "error/404";
        }
    }
    @GetMapping("/inventory/dashboard")
    public String dashboard(Model model) {
        return "inventory/dashboard";
    }

    @GetMapping("/api/inventory/overview")
    @ResponseBody
    public InventoryOverviewDTO overview() {
        return inventoryService.getOverview();
    }

    @GetMapping("/api/inventory/stats")
    @ResponseBody
    public InventoryOverviewDTO stats(@RequestParam(required = false) Integer campusId,
                                      @RequestParam(required = false) Integer categoryId,
                                      @RequestParam(required = false) String from,
                                      @RequestParam(required = false) String to) {
        return inventoryService.getStats(campusId, categoryId, from, to);
    }

    @GetMapping("/api/campuses")
    @ResponseBody
    public List<Campus> campuses() {
        return campusRepository.findAll();
    }

    @GetMapping("/api/categories")
    @ResponseBody
    public List<Category> categories() {
        return categoryRepository.findAll();
    }
}
