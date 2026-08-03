package com.swp5.library_management.controller;

import java.util.Optional;

import com.swp5.library_management.repository.BorrowTicketDetailRepository;
import com.swp5.library_management.repository.FineInvoiceRepository;
import com.swp5.library_management.repository.SystemConfigRepository;
import com.swp5.library_management.repository.CampusRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.swp5.library_management.entity.User;
import com.swp5.library_management.repository.UserRepository;

import jakarta.servlet.http.HttpSession;

@Controller
public class UserController {

    @Autowired
    private com.swp5.library_management.service.EmailService emailService;
    
    private final UserRepository userRepository;
    private final CampusRepository campusRepository;
    private final BorrowTicketDetailRepository borrowTicketDetailRepository;
    private final FineInvoiceRepository fineInvoiceRepository;
    private final SystemConfigRepository systemConfigRepository;

    UserController(UserRepository userRepository, CampusRepository campusRepository,
                   BorrowTicketDetailRepository borrowTicketDetailRepository,
                   FineInvoiceRepository fineInvoiceRepository,
                   SystemConfigRepository systemConfigRepository) {
        this.userRepository = userRepository;
        this.campusRepository = campusRepository;
        this.borrowTicketDetailRepository = borrowTicketDetailRepository;
        this.fineInvoiceRepository = fineInvoiceRepository;
        this.systemConfigRepository = systemConfigRepository;
    }
    /**
     * Hiển thị trang Hồ sơ cá nhân của người dùng đang đăng nhập.
     * Cung cấp thông tin tài khoản, trạng thái mượn trả và hiển thị popup Đổi mật khẩu.
     */
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
        session.setAttribute("loggedInUser", user);
        

        // Nếu status rỗng, hoặc CHỨA chữ "vô hiệu", hoặc CHỨA chữ "không" -> Đều coi là BỊ KHÓA
boolean isCardActive = false;
if (user.getStatus() != null) {
    String currentStatus = user.getStatus().trim().toLowerCase();
    if (currentStatus.equals("active") || currentStatus.equals("đang hoạt động")) {
        isCardActive = true;
    }
}
model.addAttribute("isCardActive", isCardActive);
        
        // --- Giữ nguyên các logic cũ chuẩn chỉ của bạn ---
        String campusName = "Unknown";
        if (user.getCampusId() != null) {
            campusName = campusRepository.findById(user.getCampusId())
                    .map(com.swp5.library_management.entity.Campus::getCampusName)
                    .orElse("Unknown");
        }
        model.addAttribute("campusName", campusName);
        
        String roleName = user.getPrimaryRole().map(r -> r.getRoleName()).orElse("Student");
        int roleId = user.getPrimaryRole().map(r -> r.getRoleId()).orElse(1);
        model.addAttribute("roleName", roleName);
        model.addAttribute("isLibrarianOrAdmin", "Librarian".equalsIgnoreCase(roleName) || "Admin".equalsIgnoreCase(roleName) || roleId == 3 || roleId == 4);
        
        // --- Borrowing Statistics & Quotas ---
        int totalBorrowed = borrowTicketDetailRepository.countActiveBorrowedByPatronId(loggedInUserId);
        int totalOverdue = borrowTicketDetailRepository.countOverdueByPatronId(loggedInUserId);
        int totalPenalties = fineInvoiceRepository.countUnpaidFinesByPatronId(loggedInUserId);
        
        int borrowLimit = 5; // Default for Student
        if ("Lecturer".equalsIgnoreCase(roleName) || "Admin".equalsIgnoreCase(roleName) || "Librarian".equalsIgnoreCase(roleName) || roleId != 1) {
             borrowLimit = systemConfigRepository.findById("MAX_BOOKS_LECTURER")
                 .map(c -> { try { return Integer.parseInt(c.getConfigValue()); } catch(Exception e) { return 10; } })
                 .orElse(10);
        } else {
             borrowLimit = systemConfigRepository.findById("MAX_BOOKS_STUDENT")
                 .map(c -> { try { return Integer.parseInt(c.getConfigValue()); } catch(Exception e) { return 5; } })
                 .orElse(5);
        }

        model.addAttribute("totalBorrowed", totalBorrowed);
        model.addAttribute("totalOverdue", totalOverdue);
        model.addAttribute("totalPenalties", totalPenalties);
        model.addAttribute("borrowLimit", borrowLimit);
        return "profile";
    }
    return "redirect:/login";
}

    @PostMapping("/profile/update-phone")
    public String updatePhone(@RequestParam("phone") String phone, HttpSession session, RedirectAttributes redirectAttributes) {
        String loggedInUserId = (String) session.getAttribute("loggedInUserId");
        if (loggedInUserId == null) {
            return "redirect:/login";
        }
        
        Optional<User> userOpt = userRepository.findById(loggedInUserId);
        if (userOpt.isPresent()) {
            if (phone != null && phone.trim().length() > 20) {
                redirectAttributes.addFlashAttribute("errorMessage", "Cập nhật thất bại: Số điện thoại quá dài (tối đa 20 ký tự)!");
                return "redirect:/profile";
            }
            if (phone != null && !phone.trim().isEmpty() && !phone.trim().matches("^[0-9\\+\\-\\s]+$")) {
                redirectAttributes.addFlashAttribute("errorMessage", "Cập nhật thất bại: Số điện thoại chứa ký tự không hợp lệ!");
                return "redirect:/profile";
            }
            
            User user = userOpt.get();
            user.setPhone(phone != null ? phone.trim() : "");
            userRepository.save(user);
            session.setAttribute("loggedInUser", user);
            redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật số điện thoại thành công!");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy thông tin người dùng!");
        }
        
        return "redirect:/profile";
    }

@GetMapping("/librarian/students/{id}/profile")
public String showStudentProfileToLibrarian(@org.springframework.web.bind.annotation.PathVariable("id") String targetUserId, HttpSession session, Model model) {
    String loggedInUserId = (String) session.getAttribute("loggedInUserId");
    if (loggedInUserId == null) {
        return "redirect:/login";
    }
    
    // Auth Check
    Optional<User> loggedInOpt = userRepository.findById(loggedInUserId);
    if (loggedInOpt.isEmpty() || (!loggedInOpt.get().isLibrarian() && !loggedInOpt.get().isAdmin())) {
        return "redirect:/"; // Not authorized
    }

    Optional<User> userOpt = userRepository.findById(targetUserId);
    if (userOpt.isPresent()) {
        User user = userOpt.get();
        model.addAttribute("user", user);
        
        boolean isCardActive = false;
        if (user.getStatus() != null) {
            String currentStatus = user.getStatus().trim().toLowerCase();
            if (currentStatus.equals("active") || currentStatus.equals("đang hoạt động")) {
                isCardActive = true;
            }
        }
        model.addAttribute("isCardActive", isCardActive);
        
        String campusName = "Unknown";
        if (user.getCampusId() != null) {
            campusName = campusRepository.findById(user.getCampusId())
                    .map(com.swp5.library_management.entity.Campus::getCampusName)
                    .orElse("Unknown");
        }
        model.addAttribute("campusName", campusName);
        
        String roleName = user.getPrimaryRole().map(r -> r.getRoleName()).orElse("Student");
        int roleId = user.getPrimaryRole().map(r -> r.getRoleId()).orElse(1);
        model.addAttribute("roleName", roleName);
        model.addAttribute("isLibrarianOrAdmin", "Librarian".equalsIgnoreCase(roleName) || "Admin".equalsIgnoreCase(roleName) || roleId == 3 || roleId == 4);
        
        int totalBorrowed = borrowTicketDetailRepository.countActiveBorrowedByPatronId(targetUserId);
        int totalOverdue = borrowTicketDetailRepository.countOverdueByPatronId(targetUserId);
        int totalPenalties = fineInvoiceRepository.countUnpaidFinesByPatronId(targetUserId);
        
        int borrowLimit = 5; 
        if ("Lecturer".equalsIgnoreCase(roleName) || "Admin".equalsIgnoreCase(roleName) || "Librarian".equalsIgnoreCase(roleName) || roleId != 1) {
             borrowLimit = systemConfigRepository.findById("MAX_BOOKS_LECTURER").map(c -> { try { return Integer.parseInt(c.getConfigValue()); } catch(Exception e) { return 10; } }).orElse(10);
        } else {
             borrowLimit = systemConfigRepository.findById("MAX_BOOKS_STUDENT").map(c -> { try { return Integer.parseInt(c.getConfigValue()); } catch(Exception e) { return 5; } }).orElse(5);
        }

        model.addAttribute("totalBorrowed", totalBorrowed);
        model.addAttribute("totalOverdue", totalOverdue);
        model.addAttribute("totalPenalties", totalPenalties);
        model.addAttribute("borrowLimit", borrowLimit);
        
        return "profile";
    }
    return "redirect:/librarian/students";
}

    // === 1. LUỒNG ĐĂNG NHẬP ===
    /**
     * Hiển thị giao diện Đăng nhập của hệ thống.
     * Hỗ trợ đăng nhập thủ công và đăng nhập qua Google OAuth2.
     */
    @GetMapping("/login")
    public String showLoginForm(@RequestParam(value = "error", required = false) String error, Model model) {
        if ("not_activated".equals(error)) {
            model.addAttribute("loginError", "Tài khoản Google của bạn chưa được kích hoạt/chưa có trên hệ thống.");
        } else if (error != null) {
            model.addAttribute("loginError", "Đăng nhập thất bại!");
        }
        return "login"; 
    }

    /**
     * Xử lý xác thực Đăng nhập thủ công bằng Mã số (User ID), Email, Mật khẩu và Cơ sở.
     * Kiểm tra trạng thái tài khoản, gán quyền (Role) vào Session và điều hướng đến Dashboard tương ứng.
     */
    @PostMapping("/login")
    public String loginUser(
            @RequestParam("userId") String userId,
            @RequestParam("password") String password,
            @RequestParam("campusId") Integer campusId,
            HttpSession session,
            RedirectAttributes redirectAttributes,
            Model model) {

        System.out.println(">>> loginUser parameters: userId=" + userId + ", campusId=" + campusId);
        Optional<User> userOpt = userRepository.findByIdentifier(userId);

        if (userOpt.isEmpty()) {
            model.addAttribute("loginError", "Tài khoản hoặc mật khẩu không chính xác!");
            model.addAttribute("userId", userId);
            model.addAttribute("campusId", campusId);
            return "login";
        }

        User user = userOpt.get();

        // ÉP BUỘC PHÂN BIỆT CHỮ HOA CHỮ THƯỜNG (Do CSDL có thể đang ở chế độ Case-Insensitive)
        boolean isUserIdMatch = user.getUserId() != null && user.getUserId().equals(userId);
        boolean isEmailMatch = user.getEmail() != null && user.getEmail().equals(userId);
        if (!isUserIdMatch && !isEmailMatch) {
            model.addAttribute("loginError", "Tài khoản hoặc mật khẩu không chính xác!");
            model.addAttribute("userId", userId);
            model.addAttribute("campusId", campusId);
            return "login";
        }
        
        if (user.getPasswordHash() == null || !user.getPasswordHash().equals(password)) {
            model.addAttribute("loginError", "Tài khoản hoặc mật khẩu không chính xác!");
            model.addAttribute("userId", userId);
            model.addAttribute("campusId", campusId);
            return "login";
        }

        if (user.getCampusId() == null || !user.getCampusId().equals(campusId)) {
            model.addAttribute("loginError", "Cơ sở học tập không chính xác!");
            model.addAttribute("userId", userId);
            model.addAttribute("campusId", campusId);
            return "login";
        }

            System.out.println("[LOGIN] User status: " + user.getStatus());
            System.out.println("[LOGIN] Roles loaded: " + user.getRoles().size() + " → " + user.getRoles());
            System.out.println("[LOGIN] isLibrarian=" + user.isLibrarian() + " | isAdmin=" + user.isAdmin());

            // Đổi từ "New" sang "Pending" để kiểm tra trạng thái kích hoạt tài khoản
            if ("Pending".equalsIgnoreCase(user.getStatus())) { 
                redirectAttributes.addFlashAttribute("infoMessage", "Tài khoản của bạn chưa kích hoạt! Vui lòng nhập mã OTP từ Email để tự đặt mật khẩu.");
                return "redirect:/activate?userId=" + user.getUserId();
            }
            
            // Chặn đăng nhập nếu tài khoản bị Vô hiệu hóa (Inactive)
            if ("Inactive".equalsIgnoreCase(user.getStatus())) {
                model.addAttribute("loginError", "Tài khoản của bạn đã bị vô hiệu hóa (Inactive) trên hệ thống!");
                return "login";
            }
            
            // 1. Lưu thông tin người dùng cơ bản vào Session
            session.setAttribute("loggedInUser", user);
            session.setAttribute("loggedInUserId", user.getUserId());
            session.setAttribute("loggedInCampusId", user.getCampusId());

            // Tạo Spring Security Session cho đăng nhập thủ công
            com.swp5.library_management.security.CustomUserDetails userDetails = new com.swp5.library_management.security.CustomUserDetails(user, new java.util.HashMap<>());
            org.springframework.security.authentication.UsernamePasswordAuthenticationToken auth = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(auth);
            redirectAttributes.addFlashAttribute("registeredName", user.getFullName());
            
            // 2. PHÂN QUYỀN NGẦM: Sử dụng hàm tiện ích trong entity
            boolean isLib = user.isLibrarian();
            boolean isAdm = user.isAdmin();
            
            // Lưu trạng thái quyền vào session phòng hờ giao diện Frontend cần dùng
            session.setAttribute("isLibrarian", isLib);
            session.setAttribute("isAdmin", isAdm);
            
            // 3. ĐIỀU HƯỚNG THÔNG MINH
            if (isAdm) {
                return "redirect:/admin/users";
            }
            if (isLib) {
                return "redirect:/librarian/inventory/dashboard"; 
            }

            System.out.println("[LOGIN] → Redirecting to /home");
            return "redirect:/home";
    }

    // === ĐĂNG XUẤT ===
    /**
     * Xử lý Đăng xuất.
     * Xóa toàn bộ dữ liệu Session hiện tại và điều hướng người dùng về trang Đăng nhập.
     */
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate(); 
        return "redirect:/login";
    }

    

    // === 4. GIAO DIỆN HIỂN THỊ MÀN HÌNH NHẬP MÃ OTP ===
    /**
     * Hiển thị giao diện Kích hoạt tài khoản lần đầu (Nhập mã OTP).
     * Áp dụng cho các tài khoản mới được Admin tạo hoặc Import từ Excel.
     */
    @GetMapping("/activate")
    public String showActivateForm(@RequestParam("userId") String userId, Model model) {
        model.addAttribute("userId", userId);
        return "activate"; // Đã sửa bỏ chữ "user/" để nhận diện file templates/activate.html
    }

    // === 5. XỬ LÝ KÍCH HOẠT TÀI KHOẢN ===
    /**
     * Xử lý xác thực mã OTP để kích hoạt tài khoản.
     * Chuyển trạng thái tài khoản từ "Pending" sang "Active" và thông báo mật khẩu mặc định.
     */
    @PostMapping("/activate")
    public String activateAccount(
            @RequestParam("userId") String userId,
            @RequestParam("otp") String inputOtp,
            RedirectAttributes redirectAttributes,
            Model model) {

        Optional<User> userOpt = userRepository.findById(userId);
        
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            String dbOtp = null;

            try {
                java.lang.reflect.Field field = User.class.getDeclaredField("activationToken");
                field.setAccessible(true);
                dbOtp = (String) field.get(user);
            } catch (Exception e) {
                System.out.println("Lỗi đọc activationToken bằng Reflection: " + e.getMessage());
            }

            if (dbOtp != null && dbOtp.equals(inputOtp.trim())) {
                // Chuyển trạng thái sang Active sau khi xác thực thành công
                user.setStatus("Active");
                
                try {
                    java.lang.reflect.Field field = User.class.getDeclaredField("activationToken");
                    field.setAccessible(true);
                    field.set(user, null);
                } catch (Exception e) {
                    // Âm thầm bỏ qua
                }

                userRepository.save(user);

                redirectAttributes.addFlashAttribute("successMessage", "Kích hoạt tài khoản thành công! Vui lòng sử dụng mật khẩu mặc định (123) để đăng nhập và tiến hành đổi mật khẩu mới.");
                return "redirect:/login";
            }
        }

        model.addAttribute("userId", userId);
        model.addAttribute("loginError", "Mã xác thực OTP không chính xác hoặc đã hết hạn!");
        return "activate"; 
    }


}