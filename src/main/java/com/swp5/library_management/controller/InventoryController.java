package com.swp5.library_management.controller;

import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.swp5.library_management.dto.AddBookForm;
import com.swp5.library_management.dto.BookDetailDTO;
import com.swp5.library_management.dto.InventoryOverviewDTO;
import com.swp5.library_management.entity.Campus;
import com.swp5.library_management.entity.Category;
import com.swp5.library_management.entity.MaterialRequest;
import com.swp5.library_management.entity.User;
import com.swp5.library_management.repository.*;
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
    private final BookService      bookService;
    private final HomeService homeService;
    private final MaterialRequestRepository materialRequestRepository;
    private final UserRepository userRepository;

    public InventoryController(InventoryService inventoryService,
                               CampusRepository campusRepository,
                               CategoryRepository categoryRepository,
                               ShelfRepository shelfRepository,
                               BookService bookService,
                               HomeService homeService,
                               MaterialRequestRepository materialRequestRepository,
                               UserRepository userRepository) {
        this.inventoryService = inventoryService;
        this.campusRepository = campusRepository;
        this.categoryRepository = categoryRepository;
        this.shelfRepository = shelfRepository;
        this.bookService = bookService;
        this.homeService = homeService;
        this.materialRequestRepository = materialRequestRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/inventory/list")
    public String listBooks(Model model) {
        model.addAttribute("majorSections", homeService.getMajorsWithRandomBooks());
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
