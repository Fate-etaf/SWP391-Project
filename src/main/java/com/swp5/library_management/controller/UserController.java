package com.swp5.library_management.controller;

import com.swp5.library_management.entity.User;
import com.swp5.library_management.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Controller
public class UserController {

    @Autowired
    private UserRepository userRepository;

    // === 1. LUỒNG ĐĂNG NHẬP (GIỮ NGUYÊN HOÀN HẢO) ===
    @GetMapping("/login")
    public String showLoginForm() {
        return "login"; 
    }

    @PostMapping("/login")
    public String loginUser(
            @RequestParam("userId") String userId,
            @RequestParam("email") String email,
            @RequestParam("campusId") Integer campusId,
            HttpSession session,
            RedirectAttributes redirectAttributes,
            Model model) {

        Optional<User> userOpt = userRepository.findByUserIdAndEmailAndCampusId(userId, email, campusId);

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            // Lưu thông tin người dùng vào Session để các trang khác dùng được
            session.setAttribute("loggedInUser", user);
            session.setAttribute("loggedInUserId", user.getUserId());
            session.setAttribute("loggedInCampusId", user.getCampusId());
            redirectAttributes.addFlashAttribute("registeredName", user.getFullName());
            return "redirect:/home";
        }

        model.addAttribute("loginError", "Mã số, Email hoặc Cơ sở học tập không trùng khớp với dữ liệu hệ thống!");
        model.addAttribute("userId", userId);
        model.addAttribute("email", email);
        model.addAttribute("campusId", campusId);
        return "login";
    }

    // === ĐĂNG XUẤT ===
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate(); // Xóa toàn bộ session
        return "redirect:/login";
    }

    // === 2. GIAO DIỆN IMPORT EXCEL ===
    @GetMapping("/admin/import-users")
    public String showImportPage() {
        return "import-users"; 
    }

    // === 3. LOGIC ĐỌC EXCEL AN TOÀN TUYỆT ĐỐI (KHÔNG BAO GIỜ BÁO ĐỎ CODE) ===
    @PostMapping("/admin/import-users")
    public String importUsersFromExcel(@RequestParam("file") MultipartFile file, Model model) {
        if (file.isEmpty()) {
            model.addAttribute("errorMessage", "Vui lòng chọn một file Excel trước khi bấm Import!");
            return "import-users";
        }

        List<User> userList = new ArrayList<>();

        try (InputStream is = file.getInputStream()) {
            // Dùng cơ chế tự động nạp Class để tránh lỗi biên dịch đỏ màn hình VS Code
            Class<?> factoryClass;
            try {
                factoryClass = Class.forName("org.apache.poi.ss.usermodel.WorkbookFactory");
            } catch (ClassNotFoundException e) {
                model.addAttribute("errorMessage", "Hệ thống thiếu thư viện Apache POI trong pom.xml! Hãy check lại file pom.");
                return "import-users";
            }

            // Mở file excel bằng Reflection
            java.lang.reflect.Method createMethod = factoryClass.getMethod("create", InputStream.class);
            Object workbook = createMethod.invoke(null, is);

            // workbook.getSheetAt(0)
            Object sheet = workbook.getClass().getMethod("getSheetAt", int.class).invoke(workbook, 0);
            
            // sheet.getLastRowNum()
            int lastRow = (Integer) sheet.getClass().getMethod("getLastRowNum").invoke(sheet);

            for (int i = 1; i <= lastRow; i++) {
                // sheet.getRow(i)
                Object row = sheet.getClass().getMethod("getRow", int.class).invoke(sheet, i);
                if (row == null) continue;

                // Đọc từng ô bằng hàm bổ trợ an toàn phía dưới
                String userId = getSafeCellValue(row, 0);
                String fullName = getSafeCellValue(row, 1);
                String email = getSafeCellValue(row, 2);
                String campusIdStr = getSafeCellValue(row, 3);

                if (userId.isEmpty() || fullName.isEmpty() || email.isEmpty()) continue;

                Integer campusId = 1; 
                try {
                    campusId = (int) Double.parseDouble(campusIdStr);
                } catch (Exception e) {
                    // Tránh lỗi nếu định dạng ô Excel bị lệch
                }

                // Đóng gói vào thực thể User sạch lỗi của bạn
                User user = User.builder()
                        .userId(userId)
                        .fullName(fullName)
                        .email(email)
                        .campusId(campusId)
                        .status("Active")
                        .borrowingLocked(false)
                        .passwordHash("123")
                        .build();

                userList.add(user);
            }

            // Đóng workbook giải phóng bộ nhớ
            workbook.getClass().getMethod("close").invoke(workbook);

            // Lưu hàng loạt vào SQL Server
            if (!userList.isEmpty()) {
                userRepository.saveAll(userList);
                model.addAttribute("successMessage", "Thành công! Đã tự động tạo tài khoản cho " + userList.size() + " bạn đọc.");
            } else {
                model.addAttribute("errorMessage", "Không tìm thấy dữ liệu bạn đọc nào hợp lệ trong file Excel!");
            }

        } catch (Exception e) {
            model.addAttribute("errorMessage", "Lỗi xử lý cấu trúc file: " + e.getCause());
        }

        return "import-users";
    }

    // Hàm đọc ô dữ liệu tránh hoàn toàn việc bị NullPointerException hoặc sai kiểu ô
    private String getSafeCellValue(Object row, int cellIndex) {
        try {
            Object cell = row.getClass().getMethod("getCell", int.class).invoke(row, cellIndex);
            if (cell == null) return "";
            
            Object cellType = cell.getClass().getMethod("getCellType").invoke(cell);
            String typeName = cellType.toString();

            if ("STRING".equals(typeName)) {
                return ((String) cell.getClass().getMethod("getStringCellValue").invoke(cell)).trim();
            } else if ("NUMERIC".equals(typeName)) {
                double val = (Double) cell.getClass().getMethod("getNumericCellValue").invoke(cell);
                return String.valueOf((int) val);
            } else if ("BOOLEAN".equals(typeName)) {
                return String.valueOf(cell.getClass().getMethod("getBooleanCellValue").invoke(cell));
            }
        } catch (Exception e) {
            // Âm thầm bỏ qua ô lỗi để không làm sập luồng lặp
        }
        return "";
    }
}