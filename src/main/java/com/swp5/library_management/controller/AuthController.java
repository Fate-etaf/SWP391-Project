package com.swp5.library_management.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.SimpleMailMessage;
import jakarta.servlet.http.HttpSession;

import com.swp5.library_management.dto.ForgotPasswordDTO;
import com.swp5.library_management.entity.User;
import com.swp5.library_management.repository.UserRepository;

import java.util.Optional;
import java.util.Random;

@Controller
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JavaMailSender mailSender;

    @GetMapping("/forgot-password")
    public String showForgotPasswordPage() {
        return "forgot-password";
    }

    @PostMapping("/api/auth/forgot-password/send-otp")
    @ResponseBody
    public ResponseEntity<?> sendForgotPasswordOtp(@RequestParam("email") String email, HttpSession session) {
        Optional<User> userOpt = userRepository.findByEmail(email.trim());
        if (!userOpt.isPresent()) {
            return ResponseEntity.badRequest().body("Địa chỉ Email không tồn tại trong hệ thống.");
        }

        String otp = String.format("%06d", new Random().nextInt(1000000));
        
        session.setAttribute("FORGOT_PASSWORD_OTP", otp);
        session.setAttribute("FORGOT_PASSWORD_EMAIL", email.trim());
        session.setAttribute("FORGOT_PASSWORD_OTP_TIME", System.currentTimeMillis());

        System.out.println("====== MÃ OTP QUÊN MẬT KHẨU CỦA " + email.trim() + " LÀ: " + otp + " ======");

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            // Google SMTP will automatically rewrite the 'From' address to the authenticated user, but we set it anyway
            message.setFrom("thuvienfpt.test@gmail.com");
            message.setTo(email.trim());
            message.setSubject("[FLMS] Mã OTP khôi phục mật khẩu");
            message.setText("Xin chào,\n\nMã OTP khôi phục mật khẩu của bạn là: " + otp + "\n\nMã này sẽ hết hạn trong 5 phút.\n\nNếu bạn không yêu cầu đổi mật khẩu, vui lòng bỏ qua email này.\n\nTrân trọng,\nFPT University Library");
            mailSender.send(message);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.ok("⚠️ Không gửi được Email do chưa cấu hình App Password. Đã bỏ qua bước gửi mail. (Bạn hãy lấy mã OTP ở cửa sổ Terminal VS Code để nhập nhé!)");
        }

        return ResponseEntity.ok("Mã OTP đã được gửi đến email của bạn.");
    }

    @PostMapping("/api/auth/forgot-password/reset")
    @ResponseBody
    public ResponseEntity<?> resetPassword(@RequestBody ForgotPasswordDTO dto, HttpSession session) {
        String sessionOtp = (String) session.getAttribute("FORGOT_PASSWORD_OTP");
        String sessionEmail = (String) session.getAttribute("FORGOT_PASSWORD_EMAIL");
        Long otpTime = (Long) session.getAttribute("FORGOT_PASSWORD_OTP_TIME");

        if (sessionOtp == null || sessionEmail == null || otpTime == null || !sessionOtp.equals(dto.getOtp())) {
            return ResponseEntity.badRequest().body("Mã OTP không chính xác hoặc đã hết hạn.");
        }

        if (!sessionEmail.equalsIgnoreCase(dto.getEmail())) {
            return ResponseEntity.badRequest().body("Thông tin email không khớp với yêu cầu OTP ban đầu.");
        }

        if (System.currentTimeMillis() - otpTime > 5 * 60 * 1000) {
            session.removeAttribute("FORGOT_PASSWORD_OTP");
            session.removeAttribute("FORGOT_PASSWORD_EMAIL");
            session.removeAttribute("FORGOT_PASSWORD_OTP_TIME");
            return ResponseEntity.badRequest().body("Mã OTP đã hết hạn.");
        }

        if (!dto.getNewPassword().equals(dto.getConfirmPassword())) {
            return ResponseEntity.badRequest().body("Xác nhận mật khẩu không khớp.");
        }

        Optional<User> userOpt = userRepository.findByEmail(sessionEmail);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setPasswordHash(dto.getNewPassword());
            userRepository.save(user);

            session.removeAttribute("FORGOT_PASSWORD_OTP");
            session.removeAttribute("FORGOT_PASSWORD_EMAIL");
            session.removeAttribute("FORGOT_PASSWORD_OTP_TIME");
            
            return ResponseEntity.ok("Khôi phục mật khẩu thành công! Bạn có thể đăng nhập ngay bây giờ.");
        }
        
        return ResponseEntity.badRequest().body("Không tìm thấy tài khoản người dùng.");
    }
}
