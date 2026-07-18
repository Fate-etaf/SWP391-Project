package com.swp5.library_management.controller;

import com.swp5.library_management.entity.MaterialRequest;
import com.swp5.library_management.entity.User;
import com.swp5.library_management.repository.CategoryRepository;
import com.swp5.library_management.repository.UserRepository;
import com.swp5.library_management.service.MaterialRequestService;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/services")
public class ServiceController {

    private final MaterialRequestService materialRequestService;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;

    public ServiceController(MaterialRequestService materialRequestService,
                             UserRepository userRepository,
                             CategoryRepository categoryRepository) {
        this.materialRequestService = materialRequestService;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
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
            @RequestParam(required = false) String isbn,
            @RequestParam String author,
            @RequestParam(required = false) String publisher,
            @RequestParam(required = false) String language,
            @RequestParam(required = false) String bookLink,
            @RequestParam(required = false) String requestUrgency,
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
            MaterialRequest request = MaterialRequest.builder()
                    .title(title)
                    .isbn(isbn)
                    .author(author)
                    .publisher(publisher)
                    .language(language)
                    .bookLink(bookLink)
                    .priority(requestUrgency)
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
}
