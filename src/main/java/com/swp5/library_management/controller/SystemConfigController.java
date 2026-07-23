package com.swp5.library_management.controller;

import com.swp5.library_management.entity.SystemConfig;
import com.swp5.library_management.entity.User;
import com.swp5.library_management.repository.SystemConfigRepository;
import com.swp5.library_management.repository.UserRepository;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/admin/settings")
@RequiredArgsConstructor
public class SystemConfigController {

    private final SystemConfigRepository systemConfigRepository;
    private final UserRepository userRepository;

    /**
     * Kiểm tra phân quyền: Chỉ cho phép Admin hoặc Thủ thư (Librarian) truy cập.
     * @param session Phiên đăng nhập hiện tại
     * @return true nếu không có quyền, false nếu hợp lệ
     */
    private boolean isNotAdminOrLibrarian(HttpSession session) {
        Boolean isLibrarian = (Boolean) session.getAttribute("isLibrarian");
        Boolean isAdmin = (Boolean) session.getAttribute("isAdmin");
        return (isLibrarian == null || !isLibrarian) && (isAdmin == null || !isAdmin);
    }

    /**
     * Hiển thị giao diện Cấu hình Hệ thống.
     * Lấy toàn bộ các tham số cấu hình (Tiền phạt, số ngày mượn...) từ Database và hiển thị ra UI.
     */
    @GetMapping
    public String viewSettings(HttpSession session, Model model) {
        if (isNotAdminOrLibrarian(session)) return "redirect:/login";

        List<SystemConfig> configs = systemConfigRepository.findAll();
        model.addAttribute("configs", configs);
        model.addAttribute("activeItem", "settings");

        return "admin/settings";
    }

    /**
     * Xử lý cập nhật giá trị của một tham số cấu hình cụ thể.
     * Kiểm tra tính hợp lệ của dữ liệu (phải là số nguyên dương) và lưu vào Database.
     */
    @PostMapping("/update")
    public String updateSetting(
            @RequestParam("configKey") String configKey,
            @RequestParam("configValue") String configValue,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        if (isNotAdminOrLibrarian(session)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Bạn không có quyền thực hiện thao tác này.");
            return "redirect:/login";
        }

        // Validate value is a non-negative integer
        try {
            int value = Integer.parseInt(configValue);
            if (value < 0) {
                redirectAttributes.addFlashAttribute("errorMessage", "Giá trị cấu hình không được là số âm!");
                return "redirect:/admin/settings";
            }
        } catch (NumberFormatException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Giá trị cấu hình phải là một số nguyên hợp lệ!");
            return "redirect:/admin/settings";
        }

        Optional<SystemConfig> configOpt = systemConfigRepository.findById(configKey);
        if (configOpt.isPresent()) {
            SystemConfig config = configOpt.get();
            config.setConfigValue(configValue);
            config.setUpdatedAt(LocalDateTime.now());

            String loggedInUserId = (String) session.getAttribute("loggedInUserId");
            if (loggedInUserId != null) {
                userRepository.findById(loggedInUserId).ifPresent(config::setUpdatedBy);
            }

            systemConfigRepository.save(config);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật cấu hình " + configKey + " thành công!");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy khóa cấu hình: " + configKey);
        }

        return "redirect:/admin/settings";
    }
}
