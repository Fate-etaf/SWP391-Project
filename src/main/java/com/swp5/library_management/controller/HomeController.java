package com.swp5.library_management.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.swp5.library_management.service.HomeService;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

/**
 * Controller cho trang chủ (Home Page) — đúng chuẩn MVC và tích hợp Google Login.
 */
@Controller
@RequiredArgsConstructor
public class HomeController {

    private final HomeService homeService;
    private final com.swp5.library_management.repository.SubjectRepository subjectRepository;
    private final com.swp5.library_management.repository.CategoryRepository categoryRepository;
    private final com.swp5.library_management.repository.MajorRepository majorRepository;

    @GetMapping({"/", "/home"})
    public String home(Model model, @AuthenticationPrincipal OAuth2User principal, HttpSession session) {
        // Nếu là Admin, kiên quyết đẩy về trang Admin Portal
        Boolean isAdmin = (Boolean) session.getAttribute("isAdmin");
        if (Boolean.TRUE.equals(isAdmin)) {
            return "redirect:/admin/users";
        }

        // Lấy dữ liệu thống kê từ hệ thống
        model.addAttribute("stats", homeService.getHomeStats());
        model.addAttribute("campuses", homeService.getCampuses());
        model.addAttribute("categories", categoryRepository.findAll());
        model.addAttribute("majors", majorRepository.findAll());
        // === XỬ LÝ ĐỒNG BỘ TRẠNG THÁI ĐĂNG NHẬP (GOOGLE & FORM TRUYỀN THỐNG) ===
        String loggedInUserId = (String) session.getAttribute("loggedInUserId");

        if (principal != null) {
            // Trường hợp 1: Đăng nhập bằng Google OAuth2 thành công
            String email = principal.getAttribute("email");
            model.addAttribute("isLoggedIn", true);
            model.addAttribute("studentName", principal.getAttribute("name"));
            model.addAttribute("studentEmail", email);
            model.addAttribute("studentPicture", principal.getAttribute("picture"));
            
            if (email != null && email.contains("@")) {
                model.addAttribute("rollNumber", email.split("@")[0].toUpperCase());
            }
        } else if (loggedInUserId != null) {
            // Trường hợp 2: Đăng nhập bằng tài khoản nội bộ (Form mượn mật khẩu cũ)
            com.swp5.library_management.entity.User user = (com.swp5.library_management.entity.User) session.getAttribute("loggedInUser");
            model.addAttribute("isLoggedIn", true);
            model.addAttribute("studentName", user != null ? user.getFullName() : "Bạn đọc");
            model.addAttribute("studentEmail", user != null ? user.getEmail() : "");
            model.addAttribute("rollNumber", loggedInUserId);
            model.addAttribute("studentPicture", "https://api.dicebear.com/7.x/bottts/svg?seed=" + loggedInUserId); // Tạo avatar mặc định đẹp nếu không có ảnh Google
        } else {
            // Trường hợp 3: Chưa đăng nhập hệ thống
            model.addAttribute("isLoggedIn", false);
        }

        Integer loggedInCampusId = (Integer) session.getAttribute("loggedInCampusId");
        model.addAttribute("featuredBooks", homeService.getFeaturedBooks(loggedInCampusId));
        return "home";
    }
}