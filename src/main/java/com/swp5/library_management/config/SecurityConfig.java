package com.swp5.library_management.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.SecurityFilterChain;
import com.swp5.library_management.repository.UserRepository;
import com.swp5.library_management.entity.User;
import java.util.Optional;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final UserRepository userRepository;

    // Inject UserRepository trực tiếp vào đây để check dữ liệu
    public SecurityConfig(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                // 1. Cho phép truy cập tự do vào các trang đăng nhập, kích hoạt và tài nguyên tĩnh
                .requestMatchers("/login", "/activate", "/oauth2/**", "/backdoor/**", "/css/**", "/js/**", "/images/**").permitAll()
                
                // 🟢 2. MỞ KHÓA TOÀN BỘ ĐƯỜNG DẪN QUẢN LÝ & IMPORT SINH VIÊN (BƯỚC 1)
                .requestMatchers("/librarian/students/**", "/librarian/students/import/**").permitAll()
                
                // 3. Tất cả các request còn lại tạm thời cho phép truy cập tự do trong chế độ phát triển
                .anyRequest().permitAll()
            )
            .oauth2Login(oauth2 -> oauth2
                .loginPage("/login")
                // --- ĐOẠN XỬ LÝ GÁN SESSION KHI ĐĂNG NHẬP GOOGLE THÀNH CÔNG ---
               .successHandler((request, response, authentication) -> {
    try {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");
        System.out.println("Google email: " + email);
        
        Optional<User> userOpt = userRepository.findByEmail(email);
        
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            System.out.println("Found user: " + user.getUserId());
            
            if ("Pending".equalsIgnoreCase(user.getStatus())) {
                user.setStatus("Active");
                userRepository.save(user);
            }
            
            jakarta.servlet.http.HttpSession session = request.getSession();
            session.removeAttribute("loginError");
            session.setAttribute("loggedInUser", user);
            session.setAttribute("loggedInUserId", user.getUserId());
            session.setAttribute("loggedInCampusId", user.getCampusId());
            
            if (user.getRole() != null) {
                String roleName = user.getRole().getRoleName();
                int roleId = user.getRole().getRoleId();
                System.out.println("User role: " + roleName + ", " + roleId);
                session.setAttribute("isLibrarian", "Librarian".equalsIgnoreCase(roleName) || roleId == 3);
                session.setAttribute("isAdmin", "Admin".equalsIgnoreCase(roleName) || roleId == 4);
                
                if ("Admin".equalsIgnoreCase(roleName) || "Librarian".equalsIgnoreCase(roleName) || roleId == 4 || roleId == 3) {
                    response.sendRedirect("/librarian/inventory/dashboard");
                    return;
                }
            }
            
            response.sendRedirect("/home");
        } else {
            System.out.println("User not found for email: " + email);
            jakarta.servlet.http.HttpSession session = request.getSession();
            session.setAttribute("loginError", "Bạn không có quyền truy cập! Tài khoản này chưa được import vào hệ thống.");
            response.sendRedirect("/login");
        }
    } catch (Exception e) {
        System.out.println("Exception in successHandler: " + e.getMessage());
        e.printStackTrace();
        response.sendRedirect("/login?error");
    }
})
            )
            .csrf(csrf -> csrf.disable());

        return http.build();
    }
}