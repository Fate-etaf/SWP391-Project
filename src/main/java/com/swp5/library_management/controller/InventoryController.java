package com.swp5.library_management.controller;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.NoSuchElementException;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import jakarta.validation.Valid;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.ResponseEntity;

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

    private static final int PAGE_SIZE = 12;

    @GetMapping("/inventory/list")
    public String listBooks(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String subjectCode,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) Integer majorId,
            @RequestParam(required = false) Integer campusId,
            @RequestParam(defaultValue = "0") int page,
            HttpSession session,
            Model model) {

        Integer librarianCampusId = (Integer) session.getAttribute("loggedInCampusId");
        boolean hasSearch = StringUtils.hasText(keyword)
                || StringUtils.hasText(subjectCode)
                || categoryId != null
                || majorId != null
                || campusId != null;

        if (hasSearch) {
            Page<BookSearchResultDTO> resultPage = bookService.searchBooks(keyword, subjectCode, categoryId, majorId,
                    campusId, librarianCampusId, page, PAGE_SIZE);
            model.addAttribute("results", resultPage.getContent());
            model.addAttribute("noResults", resultPage.isEmpty());
            model.addAttribute("totalPages", resultPage.getTotalPages());
            model.addAttribute("totalElements", resultPage.getTotalElements());
            model.addAttribute("currentPage", page);

            if (resultPage.isEmpty()) {
                model.addAttribute("majorSections", homeService.getMajorsWithRandomBooks(librarianCampusId));
            }
        } else {
            // Show all books with pagination even without search
            Page<BookSearchResultDTO> resultPage = bookService.searchBooks(null, null, null, null,
                    null, librarianCampusId, page, PAGE_SIZE);
            model.addAttribute("results", resultPage.getContent());
            model.addAttribute("noResults", resultPage.isEmpty());
            model.addAttribute("totalPages", resultPage.getTotalPages());
            model.addAttribute("totalElements", resultPage.getTotalElements());
            model.addAttribute("currentPage", page);
            model.addAttribute("hasSearch", false);
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

    /**
     * GET /librarian/inventory/import-excel → Tải về file Excel mẫu để thủ thư điền thông tin sách.
     */
    @GetMapping("/inventory/import-excel/template")
    public void downloadExcelTemplate(jakarta.servlet.http.HttpServletResponse response) throws Exception {
        byte[] templateBytes = bookService.generateImportTemplate();
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=\"book_import_template.xlsx\"");
        response.getOutputStream().write(templateBytes);
        response.getOutputStream().flush();
    }

    /**
     * POST /librarian/inventory/import-excel → Xử lý file Excel import sách hàng loạt.
     */
    @PostMapping("/inventory/import-excel")
    public String importBooksFromExcel(
            @RequestParam("file") MultipartFile file,
            HttpSession session,
            org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        if (file == null || file.isEmpty()) {
            redirectAttributes.addFlashAttribute("importError", "Vui lòng chọn file Excel để import.");
            return "redirect:/librarian/inventory/list";
        }

        String filename = file.getOriginalFilename();
        if (filename == null || (!filename.endsWith(".xlsx") && !filename.endsWith(".xls"))) {
            redirectAttributes.addFlashAttribute("importError", "File không đúng định dạng. Vui lòng upload file Excel (.xlsx hoặc .xls).");
            return "redirect:/librarian/inventory/list";
        }

        Integer campusId = (Integer) session.getAttribute("loggedInCampusId");
        if (campusId == null) {
            String loggedInUserId = (String) session.getAttribute("loggedInUserId");
            if (loggedInUserId != null) {
                User librarian = userRepository.findById(loggedInUserId).orElse(null);
                if (librarian != null) campusId = librarian.getCampusId();
            }
        }

        try {
            BookService.ImportResult result = bookService.importBooksFromExcel(file.getInputStream(), campusId);
            if (result.successCount() > 0) {
                redirectAttributes.addFlashAttribute("importSuccess",
                        "Import thành công " + result.successCount() + " cuốn sách."
                        + (result.errorCount() > 0 ? " Có " + result.errorCount() + " dòng lỗi." : ""));
            }
            if (result.errorCount() > 0 && result.successCount() == 0) {
                redirectAttributes.addFlashAttribute("importError",
                        "Import thất bại. Chi tiết lỗi: " + result.firstError());
            }
            if (!result.errors().isEmpty()) {
                redirectAttributes.addFlashAttribute("importErrors", result.errors());
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("importError", "Lỗi khi đọc file: " + e.getMessage());
        }
        return "redirect:/librarian/inventory/list";
    }

    @GetMapping("/inventory/add")
    public String showAddBookForm(HttpSession session, Model model) {
        // Resolve the librarian's campus so we only show approved requests from their campus
        String loggedInUserId = (String) session.getAttribute("loggedInUserId");
        Integer librarianCampusId = null;
        String librarianCampusName = null;
        if (loggedInUserId != null) {
            User librarian = userRepository.findById(loggedInUserId).orElse(null);
            if (librarian != null) {
                librarianCampusId = librarian.getCampusId();
                if (librarianCampusId != null) {
                    librarianCampusName = campusRepository.findById(librarianCampusId)
                            .map(c -> c.getCampusName()).orElse(null);
                }
            }
        }

        List<MaterialRequest> approvedRequests = (librarianCampusId != null)
                ? materialRequestRepository.findByStatusAndSearchTermAndCampusId("Approved", null, librarianCampusId, null, null)
                : materialRequestRepository.findByStatusAndSearchTerm("Approved", null, null, null);

        AddBookForm bookForm = new AddBookForm();
        bookForm.setCampusId(librarianCampusId);
        model.addAttribute("bookForm", bookForm);
        model.addAttribute("shelves", shelfRepository.findAll());
        model.addAttribute("approvedRequests", approvedRequests);
        model.addAttribute("librarianCampusId", librarianCampusId);
        model.addAttribute("librarianCampusName", librarianCampusName);
        return "inventory/add";
    }

    /**
     * POST /librarian/inventory/add → Lưu sách mới và redirect về danh sách.
     */
    @PostMapping("/inventory/add")
    public String saveBook(
            @Valid @ModelAttribute("bookForm") AddBookForm form,
            BindingResult bindingResult,
            jakarta.servlet.http.HttpSession session,
            org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes,
            Model model) {

        // ── Resolve librarian campus ───────────────────────────────────────────
        String loggedInUserId = (String) session.getAttribute("loggedInUserId");
        Integer librarianCampusId = null;
        String librarianCampusName = null;
        if (loggedInUserId != null) {
            User librarian = userRepository.findById(loggedInUserId).orElse(null);
            if (librarian != null) {
                librarianCampusId = librarian.getCampusId();
                if (librarianCampusId != null) {
                    librarianCampusName = campusRepository.findById(librarianCampusId)
                            .map(c -> c.getCampusName()).orElse(null);
                }
            }
        }

        // Always override campusId from librarian's campus
        form.setCampusId(librarianCampusId);
        Integer campusId = librarianCampusId;

        // ── Server-side validation ─────────────────────────────────────────────
        if (bindingResult.hasErrors()) {
            List<MaterialRequest> approvedRequests = (librarianCampusId != null)
                    ? materialRequestRepository.findByStatusAndSearchTermAndCampusId("Approved", null, librarianCampusId, null, null)
                    : materialRequestRepository.findByStatusAndSearchTerm("Approved", null, null, null);
            model.addAttribute("shelves", shelfRepository.findAll());
            model.addAttribute("approvedRequests", approvedRequests);
            model.addAttribute("librarianCampusId", librarianCampusId);
            model.addAttribute("librarianCampusName", librarianCampusName);
            return "inventory/add";
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
            List<MaterialRequest> approvedRequests = (librarianCampusId != null)
                    ? materialRequestRepository.findByStatusAndSearchTermAndCampusId("Approved", null, librarianCampusId, null, null)
                    : materialRequestRepository.findByStatusAndSearchTerm("Approved", null, null, null);

            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("shelves", shelfRepository.findAll());
            model.addAttribute("approvedRequests", approvedRequests);
            model.addAttribute("librarianCampusId", librarianCampusId);
            model.addAttribute("librarianCampusName", librarianCampusName);
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

    /** Updates a copy's physical condition and derives its availability status. */
    @PostMapping("/inventory/{id}/copies/{copyId}/condition")
    public String updateCopyCondition(
            @PathVariable Integer id,
            @PathVariable String copyId,
            @RequestParam String condition,
            @RequestParam(required = false) Integer campusId,
            HttpSession session,
            org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {

        String redirectUrl = "redirect:/librarian/inventory/" + id
                + (campusId != null ? "?campusId=" + campusId : "");

        try {
            String loggedInUserId = (String) session.getAttribute("loggedInUserId");
            User librarian = loggedInUserId == null ? null
                    : userRepository.findById(loggedInUserId).orElse(null);

            if (librarian == null || !librarian.isLibrarian()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Bạn không có quyền cập nhật trạng thái bản sao.");
                return redirectUrl;
            }
            com.swp5.library_management.entity.BookCopy copy = bookCopyRepository.findById(copyId)
                    .orElseThrow(() -> new NoSuchElementException("Không tìm thấy bản sao sách"));
            if (!id.equals(copy.getBook().getBookId())) {
                throw new IllegalArgumentException("Bản sao không thuộc đầu sách này.");
            }
            if (librarian.getCampusId() == null
                    || !librarian.getCampusId().equals(copy.getCampus().getCampusId())) {
                throw new IllegalArgumentException("Bạn chỉ có thể cập nhật bản sao tại campus của mình.");
            }
            if (!"Good".equals(condition) && !"Damaged".equals(condition) && !"Lost".equals(condition)) {
                redirectAttributes.addFlashAttribute("errorMessage", "Tình trạng vật lý của bản sao không hợp lệ.");
                return redirectUrl;
            }

            copy.setConditionStatus(condition);
            copy.setCopyStatus("Good".equals(condition) ? "Available"
                    : "Damaged".equals(condition) ? "Maintenance" : "Lost");
            bookCopyRepository.save(copy);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Đã cập nhật bản sao " + copyId + ". Tình trạng: " + condition
                            + ", trạng thái: " + copy.getCopyStatus() + ".");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }
        return redirectUrl;
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
