package com.swp5.library_management.controller;

import com.swp5.library_management.service.HomeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controller cho trang chủ (Home Page) — đúng chuẩn MVC.
 *
 * <p>Nguyên tắc: Controller chỉ làm đúng 1 việc duy nhất:
 * nhận HTTP request → uỷ thác xử lý cho Service → đưa kết quả vào Model → chỉ định View.
 * Tuyệt đối không có logic nghiệp vụ hay khởi tạo dữ liệu nào trong class này.
 *
 * <p>Sự khác biệt so với phiên bản cũ:
 * <ul>
 *   <li>TRƯỚC: Controller tự tạo HashMap, ArrayList, hardcode dữ liệu → vi phạm MVC.</li>
 *   <li>SAU: Controller chỉ inject {@link HomeService} và gọi các method của nó.</li>
 * </ul>
 *
 * <p>{@code @RequiredArgsConstructor} của Lombok tự tạo constructor inject
 * {@link HomeService} — đây là cách inject phụ thuộc được khuyến nghị
 * (Constructor Injection) thay vì {@code @Autowired} trên field.
 */
@Controller
@RequiredArgsConstructor
public class HomeController {

    private final HomeService homeService;

    @GetMapping({"/", "/home"})
    public String home(Model model, org.springframework.security.core.Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated() && !authentication.getPrincipal().equals("anonymousUser")) {
            if (authentication.getPrincipal() instanceof com.swp5.library_management.security.CustomUserDetails) {
                com.swp5.library_management.security.CustomUserDetails userDetails = (com.swp5.library_management.security.CustomUserDetails) authentication.getPrincipal();
                model.addAttribute("registeredName", userDetails.getName());
            } else if (authentication.getPrincipal() instanceof org.springframework.security.oauth2.core.user.OAuth2User) {
                org.springframework.security.oauth2.core.user.OAuth2User oauthUser = (org.springframework.security.oauth2.core.user.OAuth2User) authentication.getPrincipal();
                model.addAttribute("registeredName", oauthUser.getAttribute("name"));
            } else {
                model.addAttribute("registeredName", authentication.getName());
            }
        }
        
        model.addAttribute("stats",         homeService.getHomeStats());
        model.addAttribute("campuses",      homeService.getCampusNames());
        model.addAttribute("categories",    homeService.getFeaturedCategories(5));
        model.addAttribute("featuredBooks", homeService.getFeaturedBooks());
        return "home";
    }
}
