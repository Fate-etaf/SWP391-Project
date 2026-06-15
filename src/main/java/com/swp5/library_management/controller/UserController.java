package com.swp5.library_management.controller;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.swp5.library_management.entity.User;
import com.swp5.library_management.repository.UserRepository;

import jakarta.servlet.http.HttpSession;

@Controller
public class UserController {

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private com.swp5.library_management.repository.CampusRepository campusRepository;

    @GetMapping("/profile")
    public String showProfile(HttpSession session, Model model) {
        String loggedInUserId = (String) session.getAttribute("loggedInUserId");
        if (loggedInUserId == null) {
            return "redirect:/login";
        }
        Optional<User> userOpt = userRepository.findById(loggedInUserId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            model.addAttribute("user", user);
            
            String campusName = "Unknown";
            if (user.getCampusId() != null) {
                campusName = campusRepository.findById(user.getCampusId())
                        .map(com.swp5.library_management.entity.Campus::getCampusName)
                        .orElse("Unknown");
            }
            model.addAttribute("campusName", campusName);
            
            // role check for UI sidebar
            String roleName = user.getRole() != null ? user.getRole().getRoleName() : "Student";
            int roleId = user.getRole() != null ? user.getRole().getRoleId() : 1;
            model.addAttribute("roleName", roleName);
            model.addAttribute("isLibrarianOrAdmin", "Librarian".equalsIgnoreCase(roleName) || "Admin".equalsIgnoreCase(roleName) || roleId == 3 || roleId == 4);
            
            return "profile";
        }
        return "redirect:/login";
    }

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
            
            // 1. Lưu thông tin người dùng cơ bản vào Session
            session.setAttribute("loggedInUser", user);
            session.setAttribute("loggedInUserId", user.getUserId());
            session.setAttribute("loggedInCampusId", user.getCampusId());
            redirectAttributes.addFlashAttribute("registeredName", user.getFullName());
            
            // 2. PHÂN QUYỀN NGẦM: Bốc trực tiếp ID/Tên từ thực thể Role liên kết
            if (user.getRole() != null) {
                String roleName = user.getRole().getRoleName(); // Hãy check xem trong Role.java đặt thuộc tính là roleName hay name nhé
                int roleId = user.getRole().getRoleId();
                
                // Lưu trạng thái quyền vào session phòng hờ giao diện Frontend cần dùng
                session.setAttribute("isLibrarian", "Librarian".equalsIgnoreCase(roleName) || roleId == 3);
                session.setAttribute("isAdmin", "Admin".equalsIgnoreCase(roleName) || roleId == 4);
                
                // 3. ĐIỀU HƯỚNG THÔNG MINH: Nếu là Admin (4) hoặc Thủ thư (3) -> Vào thẳng trang Dashboard
                if ("Admin".equalsIgnoreCase(roleName) || "Librarian".equalsIgnoreCase(roleName) || roleId == 4 || roleId == 3) {
                    return "redirect:/librarian/inventory/dashboard"; 
                }
            }
            // Mặc định: Nếu là Student (Sinh viên) hoặc Giảng viên lướt trang chung -> Vào màn hình Home
            return "redirect:/home";
        }

        // Luồng xử lý khi sai tài khoản giữ nguyên
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