package com.swp5.library_management.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity; // <-- THIẾU DÒNG NÀY
import org.springframework.web.bind.annotation.*;
import java.security.Principal;

// Import DTO từ package khác sang
import com.swp5.library_management.dto.PasswordChangeDTO; 
import com.swp5.library_management.service.UserService; 

import jakarta.servlet.http.HttpSession;
import java.util.Random;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.SimpleMailMessage;

@RestController
@RequestMapping("/api/profile")
public class ProfileApiController {

    @Autowired
    private UserService userService; 

    @Autowired
    private JavaMailSender mailSender;

    @PostMapping("/send-otp")
    public ResponseEntity<?> sendOtp(Principal principal, HttpSession session) {
        if (principal == null) {
            return ResponseEntity.status(401).body("Bạn chưa đăng nhập.");
        }

        String userEmail = principal.getName();
        String otp = String.format("%06d", new Random().nextInt(1000000));
        
        session.setAttribute("PASSWORD_OTP", otp);
        session.setAttribute("PASSWORD_OTP_TIME", System.currentTimeMillis());

        System.out.println("====== MÃ OTP ĐỔI MẬT KHẨU CỦA " + userEmail + " LÀ: " + otp + " ======");

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("thuvienfpt.test@gmail.com");
            message.setTo(userEmail);
            message.setSubject("Mã OTP xác nhận đổi mật khẩu - FLMS");
            message.setText("Xin chào,\n\nMã OTP của bạn là: " + otp + "\n\nMã này sẽ hết hạn trong 5 phút.\n\nTrân trọng,\nFPT University Library");
            mailSender.send(message);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.ok("⚠️ Không gửi được Email do chưa cấu hình App Password. Đã bỏ qua bước gửi mail. (Bạn hãy lấy mã OTP ở cửa sổ Terminal VS Code để nhập nhé!)");
        }

        return ResponseEntity.ok("Mã OTP đã được gửi đến email của bạn.");
    }

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestBody PasswordChangeDTO dto, Principal principal, HttpSession session) {
        
        if (principal == null) {
            return ResponseEntity.status(401).body("Bạn chưa đăng nhập hoặc phiên làm việc hết hạn.");
        }

        String sessionOtp = (String) session.getAttribute("PASSWORD_OTP");
        Long otpTime = (Long) session.getAttribute("PASSWORD_OTP_TIME");

        if (sessionOtp == null || otpTime == null || !sessionOtp.equals(dto.getOtp())) {
            return ResponseEntity.badRequest().body("Mã OTP không chính xác hoặc đã hết hạn.");
        }

        if (System.currentTimeMillis() - otpTime > 5 * 60 * 1000) {
            session.removeAttribute("PASSWORD_OTP");
            session.removeAttribute("PASSWORD_OTP_TIME");
            return ResponseEntity.badRequest().body("Mã OTP đã hết hạn.");
        }

        if (!dto.getNewPassword().equals(dto.getConfirmPassword())) {
            return ResponseEntity.badRequest().body("Xác nhận mật khẩu không khớp.");
        }

        boolean success = userService.updatePassword(principal.getName(), dto);
        
        if (success) {
            session.removeAttribute("PASSWORD_OTP");
            session.removeAttribute("PASSWORD_OTP_TIME");
            return ResponseEntity.ok("Đổi mật khẩu thành công!");
        }
        return ResponseEntity.badRequest().body("Đổi mật khẩu thất bại.");
    }
}