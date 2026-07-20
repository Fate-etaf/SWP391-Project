package com.swp5.library_management.controller;

import com.swp5.library_management.entity.User;
import com.swp5.library_management.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class MockGoogleOAuthController {

    private final UserRepository userRepository;

    @GetMapping("/mock-google-login")
    public String showMockGoogleLogin() {
        return "mock-google-login"; // We will create this Thymeleaf template
    }

    @PostMapping("/mock-google-login")
    public String processMockLogin(@RequestParam String email, HttpServletRequest request) {
        HttpSession session = request.getSession();

        // 2. Find user in Database (Robust search for dirty Excel data with \r or trailing spaces)
        String cleanEmail = email != null ? email.trim().toLowerCase() : "";
        System.out.println("DEBUG MOCK LOGIN: Attempting to login with email = [" + cleanEmail + "]");
        List<User> allUsers = userRepository.findAll();
        System.out.println("DEBUG MOCK LOGIN: Found " + allUsers.size() + " users in DB.");
        
        Optional<User> userOpt = allUsers.stream()
                .filter(u -> {
                    if (u.getEmail() == null) return false;
                    String dbEmail = u.getEmail().trim().toLowerCase();
                    boolean match = dbEmail.equals(cleanEmail);
                    if (dbEmail.contains("mayden")) {
                        System.out.println("DEBUG MOCK LOGIN: Comparing DB Email [" + dbEmail + "] with Input [" + cleanEmail + "] -> Match? " + match);
                    }
                    return match;
                })
                .findFirst();
        
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            
            // Activate if Pending
            if ("Pending".equalsIgnoreCase(user.getStatus())) {
                user.setStatus("Active");
                userRepository.save(user);
            }
            
            // 3. Set Session Attributes
            session.removeAttribute("loginError");
            session.setAttribute("loggedInUser", user);
            session.setAttribute("loggedInUserId", user.getUserId());
            session.setAttribute("loggedInCampusId", user.getCampusId());
            
            String roleName = "User"; // default
            if (user.getPrimaryRole().isPresent()) {
                roleName = user.getPrimaryRole().get().getRoleName();
                int roleId = user.getPrimaryRole().get().getRoleId();
                session.setAttribute("isLibrarian", "Librarian".equalsIgnoreCase(roleName) || roleId == 3);
                session.setAttribute("isAdmin", "Admin".equalsIgnoreCase(roleName) || roleId == 4);
            }
            
            // 4. Update Spring Security Context (Crucial for protected routes)
            // Prefixing role with ROLE_ is a common Spring Security convention
            SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + roleName.toUpperCase());
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    email, // principal
                    null,  // credentials
                    Collections.singletonList(authority) // authorities
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // 5. Redirect based on role
            boolean isLib = user.isLibrarian();
            boolean isAdm = user.isAdmin();
            
            if (isAdm) {
                return "redirect:/admin/users";
            }
            if (isLib) {
                return "redirect:/librarian/inventory/dashboard";
            }
            
            return "redirect:/home";
        } else {
            session.setAttribute("loginError", "Bạn không có quyền truy cập! Tài khoản " + email + " chưa được import vào hệ thống.");
            return "redirect:/login";
        }
    }
}
