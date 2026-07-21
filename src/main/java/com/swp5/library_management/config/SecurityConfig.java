package com.swp5.library_management.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.SecurityFilterChain;
import com.swp5.library_management.repository.UserRepository;
import com.swp5.library_management.entity.User;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import java.util.List;
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
    public SecurityFilterChain securityFilterChain(HttpSecurity http, ClientRegistrationRepository clientRegistrationRepository) throws Exception {
        
        DefaultOAuth2AuthorizationRequestResolver customAuthorizationRequestResolver = 
            new DefaultOAuth2AuthorizationRequestResolver(clientRegistrationRepository, "/oauth2/authorization");
        customAuthorizationRequestResolver.setAuthorizationRequestCustomizer(
            customizer -> customizer.additionalParameters(params -> params.put("prompt", "select_account"))
        );

        http
            .authorizeHttpRequests(auth -> auth
                // 1. Cho phép truy cập tự do vào các trang đăng nhập, kích hoạt và tài nguyên tĩnh
                .requestMatchers("/login", "/activate", "/oauth2/**", "/mock-google-login", "/backdoor/**", "/css/**", "/js/**", "/images/**").permitAll()
                
                // 🟢 2. MỞ KHÓA TOÀN BỘ ĐƯỜNG DẪN QUẢN LÝ & IMPORT SINH VIÊN (BƯỚC 1)
                .requestMatchers("/admin/**").permitAll()
                
                // 3. Tất cả các request còn lại tạm thời cho phép truy cập tự do trong chế độ phát triển
                .anyRequest().permitAll()
            )
            .oauth2Login(oauth2 -> oauth2
                .loginPage("/login")
                .authorizationEndpoint(authEndpoint -> authEndpoint
                    .authorizationRequestResolver(customAuthorizationRequestResolver)
                )
                // --- ĐOẠN XỬ LÝ GÁN SESSION KHI ĐĂNG NHẬP GOOGLE THÀNH CÔNG ---
               .successHandler((request, response, authentication) -> {
    try {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");
        System.out.println("Google email: " + email);
        
        String cleanEmail = email != null ? email.trim().toLowerCase() : "";
        List<User> allUsers = userRepository.findAll();
        Optional<User> userOpt = allUsers.stream()
                .filter(u -> u.getEmail() != null && u.getEmail().trim().toLowerCase().equals(cleanEmail))
                .findFirst();
        
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            System.out.println("Found user: " + user.getUserId());
            
            if ("Inactive".equalsIgnoreCase(user.getStatus())) {
                jakarta.servlet.http.HttpSession session = request.getSession();
                session.setAttribute("loginError", "Tài khoản của bạn đã bị vô hiệu hóa (Inactive) trên hệ thống!");
                response.sendRedirect("/login");
                return;
            }
            
            if ("Pending".equalsIgnoreCase(user.getStatus())) {
                user.setStatus("Active");
                userRepository.save(user);
            }
            
            jakarta.servlet.http.HttpSession session = request.getSession();
            session.removeAttribute("loginError");
            session.setAttribute("loggedInUser", user);
            session.setAttribute("loggedInUserId", user.getUserId());
            session.setAttribute("loggedInCampusId", user.getCampusId());
            
            boolean isAdmin = user.isAdmin();
            boolean isLibrarian = user.isLibrarian();
            
            session.setAttribute("isAdmin", isAdmin);
            session.setAttribute("isLibrarian", isLibrarian);

            if (isAdmin) {
                response.sendRedirect("/admin/users");
                return;
            }
            if (isLibrarian) {
                response.sendRedirect("/librarian/inventory/dashboard");
                return;
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