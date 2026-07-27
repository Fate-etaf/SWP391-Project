package com.swp5.library_management.controller;

import com.swp5.library_management.entity.MaterialRequest;
import com.swp5.library_management.entity.User;
import com.swp5.library_management.repository.BookRepository;
import com.swp5.library_management.repository.CategoryRepository;
import com.swp5.library_management.repository.MaterialRequestRepository;
import com.swp5.library_management.repository.UserRepository;
import com.swp5.library_management.service.MaterialRequestService;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

@Controller
@RequestMapping("/services")
public class ServiceController {

    private final MaterialRequestService materialRequestService;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final BookRepository bookRepository;
    private final MaterialRequestRepository materialRequestRepository;

    public ServiceController(MaterialRequestService materialRequestService,
                             UserRepository userRepository,
                             CategoryRepository categoryRepository,
                             BookRepository bookRepository,
                             MaterialRequestRepository materialRequestRepository) {
        this.materialRequestService = materialRequestService;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.bookRepository = bookRepository;
        this.materialRequestRepository = materialRequestRepository;
    }

    @GetMapping("/borrowing")
    public String borrowingService() {
        return "services/borrowing";
    }

    @GetMapping("/renewal")
    public String renewalService() {
        return "services/renewal";
    }

    @GetMapping("/group-study")
    public String groupStudyService() {
        return "services/group-study";
    }

    @GetMapping("/request-material")
    public String requestMaterialForm(HttpSession session, Model model, RedirectAttributes redirectAttrs) {
        String patronId = (String) session.getAttribute("loggedInUserId");
        if (patronId == null) {
            redirectAttrs.addFlashAttribute("errorMsg", "Vui lòng đăng nhập để đề nghị tài liệu mới.");
            return "redirect:/login";
        }

        User user = userRepository.findById(patronId).orElse(null);
        if (user == null) {
            redirectAttrs.addFlashAttribute("errorMsg", "Tài khoản không tồn tại trên hệ thống.");
            return "redirect:/login";
        }
        model.addAttribute("patronId", patronId);
        model.addAttribute("userEmail", user.getEmail());
        model.addAttribute("categories", categoryRepository.findAll());
        model.addAttribute("activeNav", "service");
        return "services/request-material";
    }

    @PostMapping("/request-material")
    public String processRequestMaterial(
            @RequestParam String title,
            @RequestParam String isbn,
            @RequestParam String author,
            @RequestParam(required = false) String publisher,
            @RequestParam(required = false) String language,
            @RequestParam(required = false) String bookLink,
            @RequestParam(required = false) String priority,
            @RequestParam String reason,
            @RequestParam String email,
            @RequestParam(required = false) String feedback,
            HttpSession session,
            RedirectAttributes redirectAttrs) {

        String patronId = (String) session.getAttribute("loggedInUserId");

        if (patronId == null) {
            redirectAttrs.addFlashAttribute(
                    "errorMsg",
                    "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.");
            return "redirect:/login";
        }

        try {
            // ── Server-side: ISBN bắt buộc ──
            if (isbn == null || isbn.isBlank()) {
                redirectAttrs.addFlashAttribute("errorMsg", "Mã ISBN là bắt buộc. Vui lòng nhập mã ISBN của tài liệu.");
                return "redirect:/services/request-material";
            }

            // ── Kiểm tra ISBN đã tồn tại trong catalog sách chưa ──
            if (bookRepository.existsByIsbn(isbn.trim())) {
                redirectAttrs.addFlashAttribute("errorMsg",
                    "ISBN «" + isbn.trim() + "» đã tồn tại trong hệ thống thư viện. " +
                    "Tài liệu này đã có sẵn, bạn có thể tìm kiếm trực tiếp.");
                return "redirect:/services/request-material";
            }

            // ── Kiểm tra ISBN đã được ai đó request trước đó chưa ──
            if (materialRequestRepository.existsByIsbnIgnoreCase(isbn.trim())) {
                redirectAttrs.addFlashAttribute("errorMsg",
                    "ISBN «" + isbn.trim() + "» đã được đề nghị mua trước đó. " +
                    "Vui lòng kiểm tra trạng thái yêu cầu hoặc liên hệ thư viện.");
                return "redirect:/services/request-material";
            }

            MaterialRequest request = MaterialRequest.builder()
                    .title(title)
                    .isbn(isbn)
                    .author(author)
                    .publisher(publisher)
                    .language(language)
                    .bookLink(bookLink)
                    .priority(priority)
                    .reason(reason)
                    .email(email)
                    .feedback(feedback)
                    .build();

            materialRequestService.createMaterialRequest(patronId, request);

            redirectAttrs.addFlashAttribute(
                    "successMsg",
                    "Đề nghị mua tài liệu đã được gửi thành công.");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute(
                    "errorMsg",
                    "Đã xảy ra lỗi khi gửi yêu cầu: " + e.getMessage());
        }
        return "redirect:/services/request-material";
    }

    /**
     * AJAX endpoint – kiểm tra ISBN real-time khi user nhập vào form.
     * Trả về JSON: { "status": "ok" | "exists_book" | "exists_request" }
     */
    @GetMapping("/check-isbn")
    @ResponseBody
    public ResponseEntity<Map<String, String>> checkIsbn(@RequestParam String isbn) {
        String trimmed = isbn.trim();
        if (trimmed.isBlank()) {
            return ResponseEntity.ok(Map.of("status", "empty"));
        }
        if (bookRepository.existsByIsbn(trimmed)) {
            return ResponseEntity.ok(Map.of(
                "status", "exists_book",
                "message", "ISBN này đã tồn tại trong hệ thống thư viện. Tài liệu đã có sẵn!"
            ));
        }
        if (materialRequestRepository.existsByIsbnIgnoreCase(trimmed)) {
            return ResponseEntity.ok(Map.of(
                "status", "exists_request",
                "message", "ISBN này đã được đề nghị mua trước đó. Vui lòng kiểm tra trạng thái yêu cầu."
            ));
        }
        return ResponseEntity.ok(Map.of("status", "ok"));
    }
}
