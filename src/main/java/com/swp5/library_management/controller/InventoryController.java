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
import com.swp5.library_management.entity.User;
import com.swp5.library_management.entity.Major;
import com.swp5.library_management.entity.Subject;
import com.swp5.library_management.entity.MaterialRequest;
import com.swp5.library_management.repository.CampusRepository;
import com.swp5.library_management.repository.CategoryRepository;
import com.swp5.library_management.repository.MajorRepository;
import com.swp5.library_management.repository.ShelfRepository;
import com.swp5.library_management.repository.SubjectRepository;
import com.swp5.library_management.repository.MaterialRequestRepository;
import com.swp5.library_management.repository.UserRepository;
import com.swp5.library_management.repository.BookCopyRepository;
import com.swp5.library_management.repository.BookRepository;
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
    private final BookCopyRepository bookCopyRepository;
    private final BookRepository bookRepository;

    public InventoryController(InventoryService inventoryService,
                               CampusRepository campusRepository,
                               CategoryRepository categoryRepository,
                               ShelfRepository shelfRepository,
                               BookService bookService,
                               SubjectRepository subjectRepository,
                               MajorRepository majorRepository,
                               HomeService homeService,
                               MaterialRequestRepository materialRequestRepository,
                               UserRepository userRepository,
                               BookCopyRepository bookCopyRepository,
                               BookRepository bookRepository) {
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
        this.bookCopyRepository = bookCopyRepository;
        this.bookRepository = bookRepository;
    }

    @GetMapping("/inventory/list")
    public String listBooks(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String subjectCode,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) Integer majorId,
            @RequestParam(required = false) Integer campusId,
            HttpSession session,
            Model model) {

        Integer librarianCampusId = (Integer) session.getAttribute("loggedInCampusId");
        boolean hasSearch = StringUtils.hasText(keyword)
                || StringUtils.hasText(subjectCode)
                || categoryId != null
                || majorId != null
                || campusId != null;

        if (hasSearch) {
            List<BookSearchResultDTO> results = bookService.searchBooks(keyword, subjectCode, categoryId, majorId,
                    campusId, librarianCampusId, 0, 100).getContent();
            model.addAttribute("results", results);
            model.addAttribute("noResults", results.isEmpty());

            if (results.isEmpty()) {
                model.addAttribute("majorSections", homeService.getMajorsWithRandomBooks(librarianCampusId));
            }
        } else {
            model.addAttribute("majorSections", homeService.getMajorsWithRandomBooks(librarianCampusId));
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
    public String saveBook(
            @ModelAttribute("bookForm") AddBookForm form,
            jakarta.servlet.http.HttpSession session,
            org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes,
            Model model) {
        Integer campusId = form.getCampusId();
        if (campusId == null) {
            campusId = (Integer) session.getAttribute("loggedInCampusId");
        }

        try {
            bookService.saveBook(form, campusId);

            if (form.getRequestId() != null && !form.getRequestId().trim().isEmpty()) {
                try {
                    Integer reqId = Integer.parseInt(form.getRequestId().trim());
                    materialRequestRepository.updateStatus(reqId, "Available");
                } catch (NumberFormatException e) {
                    // Ignore if it's not a valid number
                }
            }

            redirectAttributes.addFlashAttribute("successMessage", "Thêm sách mới thành công!");
            return "redirect:/librarian/inventory/list";
        } catch (IllegalArgumentException e) {
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

            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("shelves", shelfRepository.findAll());
            model.addAttribute("approvedRequests", approvedRequests);
            return "inventory/add";
        }
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

    @PostMapping("/inventory/{id}/add-copies")
    public String addCopies(
            @PathVariable Integer id,
            @RequestParam(required = false) Integer campusId,
            @RequestParam Integer count,
            HttpSession session,
            org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        try {
            Integer effectiveCampusId = (Integer) session.getAttribute("loggedInCampusId");
            if (effectiveCampusId == null && campusId != null) {
                effectiveCampusId = campusId;
            } else if (effectiveCampusId == null) {
                String loggedInUserId = (String) session.getAttribute("loggedInUserId");
                if (loggedInUserId != null) {
                    User librarian = userRepository.findById(loggedInUserId).orElse(null);
                    if (librarian != null && librarian.getCampusId() != null) {
                        effectiveCampusId = librarian.getCampusId();
                    }
                }
            }
            if (effectiveCampusId == null) {
                effectiveCampusId = 1; // Fallback
            }

            com.swp5.library_management.entity.Book book = bookRepository.findById(id)
                    .orElseThrow(() -> new NoSuchElementException("Không tìm thấy sách"));
            com.swp5.library_management.entity.Campus campus = campusRepository.findById(effectiveCampusId)
                    .orElseThrow(() -> new NoSuchElementException("Không tìm thấy cơ sở"));
            
            com.swp5.library_management.entity.Shelf shelf = book.getShelfCode() != null 
                    ? shelfRepository.findById(book.getShelfCode()).orElse(null) : null;

            if (count == null || count <= 0) {
                redirectAttributes.addFlashAttribute("errorMessage", "Số lượng bản sao cần lớn hơn 0");
                return "redirect:/librarian/inventory/" + id;
            }

            // Tìm hậu tố lớn nhất để đặt ID dạng BOOK-{bookId}-{index}
            List<com.swp5.library_management.entity.BookCopy> existing = bookCopyRepository.findByBookBookId(id);
            int maxIndex = 0;
            String prefix = "BOOK-" + id + "-";
            for (com.swp5.library_management.entity.BookCopy copy : existing) {
                if (copy.getCopyId().startsWith(prefix)) {
                    try {
                        int index = Integer.parseInt(copy.getCopyId().substring(prefix.length()));
                        if (index > maxIndex) {
                            maxIndex = index;
                        }
                    } catch (NumberFormatException e) {
                        // ignore
                    }
                }
            }

            for (int i = 1; i <= count; i++) {
                String copyId = prefix + (maxIndex + i);
                com.swp5.library_management.entity.BookCopy copy = com.swp5.library_management.entity.BookCopy.builder()
                        .copyId(copyId)
                        .book(book)
                        .campus(campus)
                        .shelf(shelf)
                        .copyStatus("Available")
                        .conditionStatus("Good")
                        .acquiredAt(java.time.LocalDateTime.now())
                        .build();
                bookCopyRepository.save(copy);
            }

            redirectAttributes.addFlashAttribute("successMessage", "Đã thêm thành công " + count + " bản sao sách.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }
        return "redirect:/librarian/inventory/" + id;
    }

    @PostMapping("/inventory/{id}/delete-copy")
    public String deleteCopy(
            @PathVariable Integer id,
            @RequestParam String copyId,
            org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        try {
            com.swp5.library_management.entity.BookCopy copy = bookCopyRepository.findById(copyId)
                    .orElseThrow(() -> new NoSuchElementException("Không tìm thấy bản sao sách"));

            if (!"Available".equals(copy.getCopyStatus())) {
                redirectAttributes.addFlashAttribute("errorMessage", "Không thể xóa bản sao sách khi trạng thái đang là: " + copy.getCopyStatus());
                return "redirect:/librarian/inventory/" + id;
            }

            bookCopyRepository.delete(copy);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa bản sao sách " + copyId + " thành công.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }
        return "redirect:/librarian/inventory/" + id;
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

        List<String> finalSubjectCodes = subjectCodes;
        if (majorId != null && (finalSubjectCodes == null || finalSubjectCodes.isEmpty())) {
            com.swp5.library_management.entity.Major major = majorRepository.findById(majorId).orElse(null);
            if (major != null && major.getSubjects() != null && !major.getSubjects().isEmpty()) {
                finalSubjectCodes = major.getSubjects().stream()
                        .map(com.swp5.library_management.entity.Subject::getSubjectCode)
                        .collect(java.util.stream.Collectors.toList());
            } else if (major != null) {
                finalSubjectCodes = java.util.Collections.singletonList("___NONE___");
            }
        }

        // 2. Lấy toàn bộ dữ liệu Dashboard từ Service
        DashboardDataDTO data = inventoryService.getDashboardData(effectiveCampusId, finalSubjectCodes, conditions,
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
