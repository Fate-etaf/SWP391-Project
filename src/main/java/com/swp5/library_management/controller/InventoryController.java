package com.swp5.library_management.controller;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.NoSuchElementException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.swp5.library_management.dto.AddBookForm;
import com.swp5.library_management.dto.BookDetailDTO;
import com.swp5.library_management.dto.BookSearchResultDTO;
import com.swp5.library_management.dto.DashboardDataDTO;
import com.swp5.library_management.dto.InventoryOverviewDTO;
import com.swp5.library_management.entity.User;
import com.swp5.library_management.entity.Major;
import com.swp5.library_management.entity.Subject;
import com.swp5.library_management.entity.Campus;
import com.swp5.library_management.entity.Category;
import com.swp5.library_management.entity.MaterialRequest;
import com.swp5.library_management.repository.CampusRepository;
import com.swp5.library_management.repository.CategoryRepository;
import com.swp5.library_management.repository.MajorRepository;
import com.swp5.library_management.repository.ShelfRepository;
import com.swp5.library_management.repository.SubjectRepository;
import com.swp5.library_management.repository.MaterialRequestRepository;
import com.swp5.library_management.repository.UserRepository;
import com.swp5.library_management.service.BookService;
import com.swp5.library_management.service.HomeService;
import com.swp5.library_management.service.InventoryService;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/librarian")
public class InventoryController {

    private final InventoryService inventoryService;
    private final CampusRepository campusRepository;
    private final CategoryRepository categoryRepository;
    private final ShelfRepository shelfRepository;
    private final BookService bookService;
    private final SubjectRepository subjectRepository;
    private final MajorRepository majorRepository;
    private final HomeService homeService;
    private final MaterialRequestRepository materialRequestRepository;
    private final UserRepository userRepository;

    public InventoryController(InventoryService inventoryService,
                               CampusRepository campusRepository,
                               CategoryRepository categoryRepository,
                               ShelfRepository shelfRepository,
                               BookService bookService,
                               SubjectRepository subjectRepository,
                               MajorRepository majorRepository,
                               HomeService homeService,
                               MaterialRequestRepository materialRequestRepository,
                               UserRepository userRepository) {
        this.inventoryService = inventoryService;
        this.campusRepository = campusRepository;
        this.categoryRepository = categoryRepository;
        this.shelfRepository = shelfRepository;
        this.bookService = bookService;
        this.subjectRepository = subjectRepository;
        this.majorRepository = majorRepository;
        this.homeService = homeService;
        this.materialRequestRepository = materialRequestRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/inventory/list")
    public String listBooks(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String subjectCode,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) Integer majorId,
            @RequestParam(required = false) Integer campusId,
            Model model) {

        boolean hasSearch = StringUtils.hasText(keyword)
                || StringUtils.hasText(subjectCode)
                || categoryId != null
                || majorId != null
                || campusId != null;

        if (hasSearch) {
            List<BookSearchResultDTO> results = bookService.searchBooks(keyword, subjectCode, categoryId, majorId,
                    campusId, 0, 100).getContent();
            model.addAttribute("results", results);
            model.addAttribute("noResults", results.isEmpty());

            if (results.isEmpty()) {
                model.addAttribute("majorSections", homeService.getMajorsWithRandomBooks());
            }
        } else {
            model.addAttribute("majorSections", homeService.getMajorsWithRandomBooks());
        }

        model.addAttribute("campuses", campusRepository.findAll());
        model.addAttribute("subjects", subjectRepository.findAll());
        model.addAttribute("categories", categoryRepository.findAll());
        model.addAttribute("majors", majorRepository.findAll());

        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedCampusId", campusId);
        model.addAttribute("selectedSubjectCode", subjectCode);
        model.addAttribute("selectedCategoryId", categoryId);
        model.addAttribute("selectedMajorId", majorId);
        model.addAttribute("hasSearch", hasSearch);
        model.addAttribute("searchAction", "/librarian/inventory/list");

        return "inventory/list";
    }

    @GetMapping("/inventory/add")
    public String showAddBookForm(HttpSession session, Model model) {
        // Resolve the librarian's campus so we only show approved requests from their campus
        String loggedInUserId = (String) session.getAttribute("loggedInUserId");
        Integer librarianCampusId = null;
        if (loggedInUserId != null) {
            User librarian = userRepository.findById(loggedInUserId).orElse(null);
            if (librarian != null) {
                librarianCampusId = librarian.getCampusId();
            }
        }

        List<MaterialRequest> approvedRequests = (librarianCampusId != null)
                ? materialRequestRepository.findByStatusAndSearchTermAndCampusId("Approved", null, librarianCampusId)
                : materialRequestRepository.findByStatusAndSearchTerm("Approved", null);

        model.addAttribute("bookForm", new AddBookForm());
        model.addAttribute("shelves", shelfRepository.findAll());
        model.addAttribute("approvedRequests", approvedRequests);
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
            model.addAttribute("campuses", campusRepository.findAll());
            model.addAttribute("selectedCampusId", campusId);
            return "inventory/detail";
        } catch (NoSuchElementException e) {
            return "error/404";
        }
    }

    @GetMapping("/inventory/dashboard")
    public String dashboard(
            @RequestParam(required = false) Integer campusId,
            @RequestParam(required = false) List<String> subjectCodes,
            @RequestParam(required = false) List<String> conditions,
            @RequestParam(required = false) List<String> statuses,
            @RequestParam(required = false) Integer majorId,
            HttpSession session,
            Model model) {

        User user = (User) session.getAttribute("loggedInUser");
        if (user == null || !user.isLibrarian()) {
            return "redirect:/login";
        }

        // 1. Phân quyền Campus
        Integer effectiveCampusId = user.getCampusId();
        if (effectiveCampusId == null && campusId != null) {
            effectiveCampusId = campusId; // Dành cho Admin chọn campus
        } else if (effectiveCampusId == null) {
            effectiveCampusId = 1; // Fallback
        }

        // 2. Lấy toàn bộ dữ liệu Dashboard từ Service
        DashboardDataDTO data = inventoryService.getDashboardData(effectiveCampusId, subjectCodes, conditions,
                statuses);

        // 3. Đẩy dữ liệu vào Model cho Thymeleaf
        model.addAttribute("data", data);

        // 4. Đẩy danh sách tùy chọn cho các thẻ <select> (Bộ lọc)
        model.addAttribute("campuses", campusRepository.findAll());
        model.addAttribute("subjects", subjectRepository.findAll()); // Để lọc theo chuyên ngành
        
        List<Major> majors = majorRepository.findAll();
        model.addAttribute("majors", majors);
        
        Map<Integer, List<Subject>> majorSubjectMap = new HashMap<>();
        for (Major major : majors) {
            majorSubjectMap.put(major.getMajorId(), new ArrayList<>(major.getSubjects()));
        }
        model.addAttribute("majorSubjectMap", majorSubjectMap);

        // 5. Giữ lại giá trị người dùng vừa chọn trên Form
        model.addAttribute("selectedCampusId", effectiveCampusId);

        return "inventory/dashboard";
    }
}
