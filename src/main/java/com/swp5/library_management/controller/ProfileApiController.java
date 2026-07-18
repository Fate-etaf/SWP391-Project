package com.swp5.library_management.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity; // <-- THIẾU DÒNG NÀY
import org.springframework.web.bind.annotation.*;
import java.security.Principal;

// Import DTO từ package khác sang
import com.swp5.library_management.dto.PasswordChangeDTO; // <-- THIẾU DÒNG NÀY
import com.swp5.library_management.service.UserService; // Đảm bảo đã import đúng UserService của bạn

@RestController
@RequestMapping("/api/profile")
public class ProfileApiController {

    @Autowired
    private UserService userService; 

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestBody PasswordChangeDTO dto, Principal principal) {
        
        // Lưu ý: Hệ thống phải dùng Spring Security thì principal mới không bị null
        if (principal == null) {
            return ResponseEntity.status(401).body("Bạn chưa đăng nhập hoặc phiên làm việc hết hạn.");
        }

        // Gọi xuống Service để xử lý (Cần khai báo hàm này trong UserService - Xem bước tiếp theo bên dưới)
        boolean success = userService.updatePassword(principal.getName(), dto);
        
        if (success) {
            return ResponseEntity.ok("Đổi mật khẩu thành công!");
        }
        return ResponseEntity.badRequest().body("Mật khẩu cũ không đúng hoặc xác nhận mật khẩu mới chưa khớp.");
    }
}