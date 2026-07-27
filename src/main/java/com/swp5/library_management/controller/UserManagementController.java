package com.swp5.library_management.controller;

import com.swp5.library_management.entity.User;
import com.swp5.library_management.entity.Role;
import com.swp5.library_management.entity.Notification;
import com.swp5.library_management.repository.UserRepository;
import com.swp5.library_management.repository.NotificationRepository;
import com.swp5.library_management.service.UserStatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
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
import java.time.LocalDateTime;

@Controller
@RequiredArgsConstructor
public class UserManagementController {

    private final UserRepository userRepository;
    private final JavaMailSender mailSender;
    private final UserStatusService userStatusService;
    private final com.swp5.library_management.repository.BorrowTicketRepository borrowTicketRepository;
    private final NotificationRepository notificationRepository;

    private boolean isNotAdmin(jakarta.servlet.http.HttpSession session) {
        Boolean isAdmin = (Boolean) session.getAttribute("isAdmin");
        return isAdmin == null || !isAdmin;
    }

    /**
     * Hiển thị giao diện Quản lý Người dùng dành cho Admin.
     * Cung cấp danh sách người dùng kèm bộ lọc theo trạng thái, cơ sở học tập (Campus), và thanh tìm kiếm.
     */
    @GetMapping("/admin/users")
    public String manageStudents(
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "campusId", required = false) Integer campusId,
            @RequestParam(value = "tab", defaultValue = "all") String tab,
            @RequestParam(value = "computedStatus", required = false) String computedStatus,
            @RequestParam(value = "page", defaultValue = "1") int page,
            jakarta.servlet.http.HttpSession session,
            Model model) {
        
        if (isNotAdmin(session)) return "redirect:/login";

        
        List<User> allUsers = userRepository.findAll();
        java.util.stream.Stream<User> stream = allUsers.stream();
        
        if (search != null && !search.trim().isEmpty()) {
            String cleanSearch = search.trim().toLowerCase();
            stream = stream.filter(u -> (u.getFullName() != null && u.getFullName().toLowerCase().contains(cleanSearch))
                                     || (u.getUserId() != null && u.getUserId().toLowerCase().contains(cleanSearch))
                                     || (u.getEmail() != null && u.getEmail().toLowerCase().contains(cleanSearch)));
        }
        
        if (campusId != null) {
            stream = stream.filter(u -> campusId.equals(u.getCampusId()));
        }
        
        if ("all".equals(tab)) {
            // Không filter theo role, lấy toàn bộ
        } else if ("lecturers".equals(tab)) {
            stream = stream.filter(u -> u.getRoles() != null && u.getRoles().stream().anyMatch(r -> r.getRoleId() == 2));
        } else if ("librarians".equals(tab)) {
            stream = stream.filter(u -> u.getRoles() != null && u.getRoles().stream().anyMatch(r -> r.getRoleId() == 3));
        } else if ("admins".equals(tab)) {
            stream = stream.filter(u -> u.getRoles() != null && u.getRoles().stream().anyMatch(r -> r.getRoleId() == 4));
        } else if ("graduated_only".equals(tab)) {
            stream = stream.filter(u -> "Graduated".equalsIgnoreCase(u.getStatus()));
        } else if ("graduates".equals(tab)) {
            stream = stream.filter(u -> "Inactive".equalsIgnoreCase(u.getStatus()) 
                                     || "Graduated".equalsIgnoreCase(u.getStatus())
                                     || Boolean.TRUE.equals(u.getBorrowingLocked()));
        } else {
            // default is "students"
            stream = stream.filter(u -> (u.getRoles() == null || u.getRoles().isEmpty() || u.getRoles().stream().anyMatch(r -> r.getRoleId() == 1)) && !"Inactive".equalsIgnoreCase(u.getStatus()) && !"Graduated".equalsIgnoreCase(u.getStatus()));
        }
        
        List<User> filteredStudents = stream.collect(java.util.stream.Collectors.toList());
        userStatusService.enrichStatuses(filteredStudents);

        if (computedStatus != null && !computedStatus.trim().isEmpty()) {
            filteredStudents = filteredStudents.stream()
                .filter(u -> computedStatus.equalsIgnoreCase(u.getComputedStatus()))
                .collect(java.util.stream.Collectors.toList());
        }

        // Pagination Logic
        int pageSize = 20;
        int totalItems = filteredStudents.size();
        int totalPages = (int) Math.ceil((double) totalItems / pageSize);
        if (totalPages == 0) totalPages = 1;
        if (page < 1) page = 1;
        if (page > totalPages) page = totalPages;
        
        int startItem = (page - 1) * pageSize;
        List<User> pagedStudents;
        if (filteredStudents.size() < startItem) {
            pagedStudents = java.util.Collections.emptyList();
        } else {
            int toIndex = Math.min(startItem + pageSize, filteredStudents.size());
            pagedStudents = filteredStudents.subList(startItem, toIndex);
        }

        model.addAttribute("students", pagedStudents);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("currentSearch", search);
        model.addAttribute("currentCampusId", campusId);
        model.addAttribute("currentTab", tab);
        model.addAttribute("currentComputedStatus", computedStatus);
        
        return "admin/users";
    }

    /**
     * Xử lý thêm người dùng (Sinh viên/Giảng viên/Thủ thư/Admin) thủ công.
     * Tạo tài khoản, cấu hình mặc định (Active, mật khẩu "123", ...) và kích hoạt tiến trình gửi Email.
     */
    @PostMapping("/admin/users/add-manual")
    public String addManualStudent(
            @RequestParam("userId") String userId,
            @RequestParam("fullName") String fullName,
            @RequestParam(value = "email", required = false) String email,
            @RequestParam("campusId") Integer campusId,
            @RequestParam(value = "roleId", defaultValue = "1") Integer roleId,
            @RequestParam(value = "status", defaultValue = "Active") String status,
            jakarta.servlet.http.HttpSession session,
            RedirectAttributes redirectAttributes) {
        
        if (isNotAdmin(session)) return "redirect:/login";

        
        // Kiểm tra định dạng Mã định danh theo Role
        String upperUserId = userId.trim().toUpperCase();
        if (roleId == 1) { 
            // Nếu là Sinh viên
            if (!upperUserId.matches("^(HE|HS)\\d+$")) {
                redirectAttributes.addFlashAttribute("errorMessage", "Thêm thất bại: Mã Sinh viên bắt buộc phải bắt đầu bằng 'HE' hoặc 'HS' kèm theo các chữ số (VD: HE150000)!");
                return "redirect:/admin/users";
            }
        } else {
            // Nếu là Giảng viên, Admin, Thủ thư
            if (upperUserId.startsWith("HE") || upperUserId.startsWith("HS")) {
                redirectAttributes.addFlashAttribute("errorMessage", "Thêm thất bại: Mã định danh của Cán bộ/Giảng viên không được dùng tiền tố HE hoặc HS của sinh viên!");
                return "redirect:/admin/users";
            }
            if (!upperUserId.matches("^[A-Z0-9]+$")) {
                redirectAttributes.addFlashAttribute("errorMessage", "Mã định danh không hợp lệ (chỉ chấp nhận chữ cái và số)!");
                return "redirect:/admin/users";
            }
        }

        if (!fullName.matches("^[\\p{L}\\s]+$")) {
            redirectAttributes.addFlashAttribute("errorMessage", "Họ và tên không hợp lệ (không chứa số hoặc ký tự đặc biệt)!");
            return "redirect:/admin/users";
        }

        Optional<User> existingUser = userRepository.findById(userId);
        if (existingUser.isPresent()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Mã người dùng đã tồn tại trong hệ thống!");
            return "redirect:/admin/users";
        }
        
        if (email != null && !email.trim().isEmpty()) {
            if (!email.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")) {
                redirectAttributes.addFlashAttribute("errorMessage", "Định dạng Email không hợp lệ!");
                return "redirect:/admin/users";
            }
            if (userRepository.existsByEmail(email.trim())) {
                redirectAttributes.addFlashAttribute("errorMessage", "Email này đã được sử dụng cho một tài khoản khác!");
                return "redirect:/admin/users";
            }
        }
        
        User newUser = new User();
        newUser.setUserId(userId.trim().toUpperCase());
        newUser.setFullName(fullName.trim());
        newUser.setCampusId(campusId);
        newUser.setStatus(status);
        newUser.setBorrowingLocked(false);
        
        Role userRole = new Role();
        userRole.setRoleId(roleId); 
        newUser.setRole(userRole);
        if (newUser.getRoles() == null) {
            newUser.setRoles(new java.util.HashSet<>());
        }
        newUser.getRoles().add(userRole);
        
        newUser.setPasswordHash("12345678");
        
        if (email != null && !email.trim().isEmpty()) {
            newUser.setEmail(email.trim());
        } else {
            newUser.setEmail(userId.trim().toLowerCase() + "@fpt.edu.vn"); 
        }
        
        userRepository.save(newUser);
        
        // Gửi email kích hoạt tự động chạy ngầm
        final String finalEmail = newUser.getEmail();
        if (finalEmail != null && !finalEmail.isEmpty()) {
            new Thread(() -> {
                try {
                    SimpleMailMessage message = new SimpleMailMessage();
                    message.setFrom("thuvienfpt.test@gmail.com"); 
                    message.setTo(finalEmail);
                    message.setSubject("[FLMS FPT Library] Thông báo kích hoạt tài khoản thư viện số");
                    message.setText("Xin chào bạn,\n\nTài khoản thư viện số FLMS của bạn trên hệ thống đã được kích hoạt thành công bởi Ban quản trị.\nBây giờ bạn đã có thể truy cập hệ thống và thực hiện mượn trả tài liệu.\n\nMật khẩu đăng nhập tạm thời của bạn là: 12345678\nVui lòng đăng nhập và đổi mật khẩu để bảo mật tài khoản.\n\nTrân trọng,\nBan quản lý thư viện Đại học FPT.");
                    mailSender.send(message);
                } catch (Exception e) {
                    System.out.println("Lỗi gửi mail đến: " + finalEmail + " -> " + e.getMessage());
                }
            }).start();
        }

        redirectAttributes.addFlashAttribute("successMessage", "Thêm mới thủ công tài khoản: " + userId.trim().toUpperCase() + " thành công và đã gửi mail thông báo!");
        
        return "redirect:/admin/users";
    }

    //Hung
    // === 4. CHỈNH SỬA THÔNG TIN NGƯỜI DÙNG ===
    /**
     * Cho phép Admin cập nhật Họ tên, Email, Cơ sở, Chức vụ của một tài khoản bất kỳ.
     * Đã được tinh gọn: Lược bỏ phần cập nhật "Trạng thái thẻ" ở form này
     * để nhường chức năng đó cho Nút Ổ khóa (changeStatusModal) xử lý.
     */
    @PostMapping("/admin/users/edit")
    public String editStudent(
            @RequestParam("userId") String userId,
            @RequestParam("fullName") String fullName,
            @RequestParam("email") String email,
            @RequestParam("campusId") Integer campusId,
            @RequestParam("roleId") Integer roleId,
            jakarta.servlet.http.HttpSession session,
            RedirectAttributes redirectAttributes) {
        
        if (isNotAdmin(session)) return "redirect:/login";

        
        if (!fullName.matches("^[\\p{L}\\s]+$")) {
            redirectAttributes.addFlashAttribute("errorMessage", "Cập nhật thất bại: Họ và tên không hợp lệ!");
            return "redirect:/admin/users";
        }

        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            
            if (email == null || email.trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Cập nhật thất bại: Email không được để trống!");
                return "redirect:/admin/users";
            }
            
            if (!email.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")) {
                redirectAttributes.addFlashAttribute("errorMessage", "Cập nhật thất bại: Định dạng Email không hợp lệ!");
                return "redirect:/admin/users";
            }
            Optional<User> emailOwnerOpt = userRepository.findByEmail(email.trim());
            if (emailOwnerOpt.isPresent() && !emailOwnerOpt.get().getUserId().equalsIgnoreCase(userId)) {
                redirectAttributes.addFlashAttribute("errorMessage", "Cập nhật thất bại: Email này đã được sử dụng cho một tài khoản khác!");
                return "redirect:/admin/users";
            }
            
            user.setFullName(fullName.trim());
            user.setEmail(email.trim());
            user.setCampusId(campusId);
            
            Role userRole = new Role();
            userRole.setRoleId(roleId);
            user.setRole(userRole); // Cập nhật luôn thuộc tính role (ManyToOne)
            user.getRoles().clear();
            user.getRoles().add(userRole);
            
            userRepository.save(user);
            
            redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật thông tin tài khoản: " + userId + " thành công!");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy người dùng để cập nhật!");
        }
        
        return "redirect:/admin/users";
    }

    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    /**
     * Xử lý xóa vĩnh viễn một người dùng khỏi hệ thống.
     * Sẽ bị chặn (hiện lỗi) nếu người dùng đang có sách mượn (đang có BorrowTicket kích hoạt).
     */
    @PostMapping("/admin/users/delete")
    @org.springframework.transaction.annotation.Transactional
    public String deleteStudent(
            @RequestParam("userId") String userId,
            jakarta.servlet.http.HttpSession session,
            RedirectAttributes redirectAttributes) {
        
        if (isNotAdmin(session)) return "redirect:/login";

        
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            int activeBorrowCount = borrowTicketRepository.countByPatronUserId(userId);
            if (activeBorrowCount > 0) {
                redirectAttributes.addFlashAttribute("errorMessage", "Không thể xóa! Người dùng này đang có sách chưa trả. Vui lòng sử dụng tính năng Khóa Thẻ.");
            } else {
                try {
                    // Force Delete: Xóa tất cả các bản ghi liên quan (Lịch sử) trước khi xóa User
                    jdbcTemplate.update("DELETE FROM dbo.UserRoles WHERE UserID = ?", userId);
                    jdbcTemplate.update("DELETE FROM dbo.Notifications WHERE UserID = ?", userId);
                    
                    // Xóa chi tiết mượn và đơn mượn
                    jdbcTemplate.update("DELETE FROM BorrowTicketDetails WHERE TicketID IN (SELECT TicketID FROM BorrowTickets WHERE PatronID = ?)", userId);
                    jdbcTemplate.update("DELETE FROM BorrowTickets WHERE PatronID = ?", userId);
                    
                    // Xóa hóa đơn phạt
                    jdbcTemplate.update("DELETE FROM FineInvoices WHERE PatronID = ?", userId);
                    
                    // Xóa các dữ liệu khác (nếu có)
                    jdbcTemplate.update("DELETE FROM dbo.Reservations WHERE PatronID = ?", userId);
                    jdbcTemplate.update("DELETE FROM dbo.Waitlists WHERE PatronID = ?", userId);
                    jdbcTemplate.update("DELETE FROM dbo.RoomBookings WHERE PatronID = ?", userId);
                    jdbcTemplate.update("DELETE FROM dbo.MaterialRequests WHERE PatronID = ?", userId);
                    
                    // Cuối cùng xóa User
                    userRepository.deleteById(userId);
                    redirectAttributes.addFlashAttribute("successMessage", "Đã xóa hoàn toàn tài khoản và mọi lịch sử liên quan của: " + userId + " khỏi hệ thống!");
                } catch (Exception e) {
                    redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi xóa tài khoản: " + e.getMessage());
                }
            }
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy người dùng để xóa!");
        }
        
        return "redirect:/admin/users";
    }

    /**
     * Hiển thị giao diện Import người dùng hàng loạt từ File Excel.
     */
    @GetMapping("/admin/users/import")
    public String showImportPage(jakarta.servlet.http.HttpSession session) {
        if (isNotAdmin(session)) return "redirect:/login";
        return "admin/users-import"; 
    }

    /**
     * Xử lý đọc File Excel và tạo người dùng hàng loạt.
     * Phân loại theo Sinh viên/Giảng viên, bỏ qua các dữ liệu trùng lặp (trùng Email/Mã số).
     * Tạo tiến trình ngầm (Thread) để gửi Email thông báo mật khẩu hàng loạt.
     */
    @PostMapping("/admin/users/import/process")
    public String processExcelUpload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("importType") String importType,
            jakarta.servlet.http.HttpSession session,
            RedirectAttributes redirectAttributes) {

        if (isNotAdmin(session)) return "redirect:/login";


        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Vui lòng chọn một file Excel trước khi bấm xử lý!");
            return "redirect:/admin/users/import";
        }

        List<User> usersToSave = new ArrayList<>();
        List<String> emailsToNotify = new ArrayList<>(); // Lưu email tài khoản mới phục vụ gửi mail ngầm
        java.util.Set<String> processedEmails = new java.util.HashSet<>(); // Chặn email trùng nội bộ trong file Excel
        int successCount = 0;

        try (InputStream is = file.getInputStream();
             org.apache.poi.ss.usermodel.Workbook workbook = org.apache.poi.ss.usermodel.WorkbookFactory.create(is)) {

            org.apache.poi.ss.usermodel.Sheet sheet = workbook.getSheetAt(0);
            int lastRow = sheet.getLastRowNum();

            for (int i = 1; i <= lastRow; i++) {
                org.apache.poi.ss.usermodel.Row row = sheet.getRow(i);
                if (row == null) continue;

                String userId = getSafeCellValue(row, 0).toUpperCase();
                String fullName = getSafeCellValue(row, 1);
                String email = getSafeCellValue(row, 2);
                String campusIdStr = getSafeCellValue(row, 3);

                if (userId.isEmpty()) continue;

                Integer campusId = 1; 
                try {
                    if (!campusIdStr.isEmpty()) {
                        campusId = (int) Double.parseDouble(campusIdStr);
                    }
                } catch (Exception e) {}

                Optional<User> userOpt = userRepository.findById(userId);

                if ("GRADUATED".equalsIgnoreCase(importType)) {
                    if (userOpt.isPresent()) {
                        User student = userOpt.get();
                        
                        // Chỉ cập nhật nếu trạng thái hiện tại chưa phải là Graduated
                        if (!"Graduated".equalsIgnoreCase(student.getStatus())) {
                            student.setStatus("Graduated");
                            student.setBorrowingLocked(false);
                            usersToSave.add(student);
                            successCount++;
                            
                            // Tạo thông báo
                            Notification notif = Notification.builder()
                                .user(student)
                                .notificationType("STATUS_UPDATE")
                                .title("Cập nhật trạng thái Tốt Nghiệp")
                                .content("Tài khoản của bạn đã được chuyển sang trạng thái Đã Tốt Nghiệp. Bạn sẽ không thể mượn thêm sách nhưng vẫn có thể tra cứu lịch sử.")
                                .status("Pending")
                                .createdAt(LocalDateTime.now())
                                .read(false)
                                .build();
                            notificationRepository.save(notif);
                        }
                    }
                } else {
                    if (userOpt.isPresent()) {
                        continue; // Bỏ qua trùng lặp theo đúng chuẩn Use Case E1
                    }
                    
                    User account = new User();
                    
                    if (email != null && !email.trim().isEmpty()) {
                        String checkEmail = email.trim().toLowerCase();
                        if (userRepository.existsByEmail(checkEmail) || processedEmails.contains(checkEmail)) {
                            continue; // Bỏ qua nếu email đã tồn tại trong DB hoặc bị trùng với dòng trước đó trong file Excel
                        }
                        processedEmails.add(checkEmail);
                        emailsToNotify.add(checkEmail);
                    }

                    account.setUserId(userId);
                    
                    if (fullName.isEmpty()) {
                        account.setFullName("LECTURER".equalsIgnoreCase(importType) ? "Giảng viên FPT" : "Người dùng FPT");
                    } else {
                        account.setFullName(fullName);
                    }
                    
                    account.setEmail(email);
                    account.setCampusId(campusId);
                    account.setStatus("Active");
                    account.setBorrowingLocked(false);
                    
                    Role targetRole = new Role();
                    if ("LECTURER".equalsIgnoreCase(importType)) {
                        targetRole.setRoleId(2); // Gán quyền Giảng viên ngầm định
                        account.setRole(targetRole);
                        if (account.getRoles() == null) account.setRoles(new java.util.HashSet<>());
                        account.getRoles().add(targetRole);
                    } else if (!userOpt.isPresent()) {
                        targetRole.setRoleId(1); // Gán quyền Sinh viên cho tài khoản tạo mới
                        account.setRole(targetRole);
                        if (account.getRoles() == null) account.setRoles(new java.util.HashSet<>());
                        account.getRoles().add(targetRole);
                    }

                    account.setPasswordHash("12345678");  
                    
                    usersToSave.add(account);
                    successCount++;
                }
            }
            
            int skippedCount = (lastRow) - successCount;

            if (!usersToSave.isEmpty()) {
                userRepository.saveAll(usersToSave);
                
                // Luồng gửi email kích hoạt tự động chạy ngầm tránh lag trình duyệt
                if (("NEW".equalsIgnoreCase(importType) || "LECTURER".equalsIgnoreCase(importType)) && !emailsToNotify.isEmpty()) {
                    new Thread(() -> {
                        for (String recipientEmail : emailsToNotify) {
                            try {
                                SimpleMailMessage message = new SimpleMailMessage();
                                message.setFrom("thuvienfpt.test@gmail.com"); 
                                message.setTo(recipientEmail);
                                message.setSubject("[FLMS FPT Library] Thông báo kích hoạt tài khoản thư viện số");
                                message.setText("Xin chào bạn,\n\nTài khoản thư viện số FLMS của bạn trên hệ thống đã được kích hoạt thành công bởi Ban quản trị.\nBây giờ bạn đã có thể truy cập hệ thống và thực hiện mượn trả tài liệu.\n\nMật khẩu đăng nhập tạm thời của bạn là: 12345678\nVui lòng đăng nhập và đổi mật khẩu để bảo mật tài khoản.\n\nTrân trọng,\nBan quản lý thư viện Đại học FPT.");
                                mailSender.send(message);
                            } catch (Exception e) {
                                System.out.println("Lỗi gửi mail đến: " + recipientEmail + " -> " + e.getMessage());
                            }
                        }
                    }).start();
                }

                // Cấu hình nhãn chuỗi chữ thông báo động hiển thị trên UI
                String typeText = "LECTURER".equalsIgnoreCase(importType) ? "Giảng viên mới" : 
                                 ("NEW".equalsIgnoreCase(importType) ? "Người dùng mới" : "Sinh viên tốt nghiệp");
                                 
                redirectAttributes.addFlashAttribute("successMessage", 
                    "Thành công! Đã xử lý đợt [" + typeText + "]. Tạo mới/Cập nhật " + successCount + " tài khoản, Bỏ qua " + skippedCount + " tài khoản bị trùng lặp.");
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy dữ liệu nào cần cập nhật trong file Excel!");
            }

        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi cấu trúc hoặc định dạng file: " + e.getMessage());
        }

        return "redirect:/admin/users";
    }

    /**
     * Bật/Tắt khóa thẻ mượn của Sinh viên/Giảng viên (Khóa cưỡng chế - Hard Lock).
     * Khi khóa, người dùng sẽ không thể mượn sách, dù chưa đạt giới hạn mượn hay không bị phạt.
     */
    @GetMapping("/admin/users/toggle-lock/{id}")
    public String toggleUserLock(
            @org.springframework.web.bind.annotation.PathVariable("id") String userId, 
            jakarta.servlet.http.HttpSession session,
            RedirectAttributes redirectAttributes) {
        
        if (isNotAdmin(session)) return "redirect:/login";

        
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User student = userOpt.get();
            boolean isLocked = student.getBorrowingLocked() != null ? student.getBorrowingLocked() : false;
            student.setBorrowingLocked(!isLocked);
            
            // Auto-activate if unlocking an inactive user
            if (isLocked && "Inactive".equalsIgnoreCase(student.getStatus())) {
                student.setStatus("Active");
            }
            
            userRepository.save(student);
            
            // Send Notification
            String statusText = isLocked ? "MỞ KHÓA" : "KHÓA THẺ";
            Notification notif = Notification.builder()
                    .user(student)
                    .notificationType(isLocked ? "ACCOUNT_UNLOCKED" : "ACCOUNT_LOCKED")
                    .title(isLocked ? "Tài khoản đã được mở khóa" : "Tài khoản bị khóa mượn sách")
                    .content(isLocked ? "Tài khoản của bạn đã được mở khóa. Bạn có thể tiếp tục sử dụng các dịch vụ thư viện." : "Tài khoản của bạn đã bị khóa quyền mượn sách do vi phạm nội quy thư viện. Vui lòng liên hệ thủ thư để biết thêm chi tiết.")
                    .status("Pending")
                    .createdAt(LocalDateTime.now())
                    .read(false)
                    .build();
            notificationRepository.save(notif);
            
            redirectAttributes.addFlashAttribute("successMessage", "Đã " + statusText + " tài khoản mã số: " + userId + " thành công!");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy tài khoản người dùng!");
        }
        return "redirect:/admin/users";
    }

    //Hung
    // === 5. CẬP NHẬT TRẠNG THÁI NGƯỜI DÙNG (NÚT Ổ KHÓA) ===
    /**
     * Xử lý khi Admin/Thủ thư click vào Nút Ổ khóa để đổi trạng thái tài khoản.
     * Có tích hợp:
     * - Tự động xóa token để kick User đang đăng nhập ra khỏi hệ thống nếu bị Khóa thẻ (Inactive/Graduated).
     * - Gửi Email chúc mừng nếu trạng thái đổi sang "Tốt nghiệp".
     */
    @PostMapping("/admin/users/change-status-modal")
    @org.springframework.transaction.annotation.Transactional
    public String changeStatusModal(
            @RequestParam("userId") String userId,
            @RequestParam("newStatus") String newStatus,
            jakarta.servlet.http.HttpSession session,
            RedirectAttributes redirectAttributes) {
            
        if (isNotAdmin(session)) return "redirect:/login";

        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            String oldStatus = user.getStatus();
            
            // Handle statuses
            if ("Active".equalsIgnoreCase(newStatus)) {
                user.setStatus("Active");
                user.setBorrowingLocked(false); // Unlocks any punishment
            } else if ("Inactive".equalsIgnoreCase(newStatus)) {
                user.setStatus("Inactive");
                user.setBorrowingLocked(false);
            } else if ("Graduated".equalsIgnoreCase(newStatus)) {
                boolean isStudent = user.getRoles() != null && user.getRoles().stream().anyMatch(r -> r.getRoleId() == 1);
                if (!isStudent) {
                    redirectAttributes.addFlashAttribute("errorMessage", "Cập nhật thất bại: Trạng thái 'Tốt nghiệp' chỉ áp dụng cho Sinh viên!");
                    return "redirect:/admin/users";
                }
                user.setStatus("Graduated");
                user.setBorrowingLocked(false);
            } else if ("BorrowingLocked".equalsIgnoreCase(newStatus)) {
                user.setStatus("Active");
                user.setBorrowingLocked(true);
            }
            
            userRepository.save(user);

            //Hung: Khởi tạo thông báo tự động (Notification) dựa trên trạng thái mới
            // Gửi thông báo nếu trạng thái thực sự thay đổi
            if (!user.getStatus().equalsIgnoreCase(oldStatus)) {
                String title = "";
                String content = "";
                
                if ("Active".equalsIgnoreCase(user.getStatus())) {
                    title = "Kích Hoạt Tài Khoản";
                    content = "Tài khoản của bạn đã được chuyển sang trạng thái Hoạt Động bình thường. Bạn có thể sử dụng tất cả các dịch vụ của thư viện.";
                } else if ("Inactive".equalsIgnoreCase(user.getStatus())) {
                    title = "Vô Hiệu Hóa Tài Khoản";
                    content = "Tài khoản của bạn đã bị Vô hiệu hóa (Không hoạt động). Vui lòng liên hệ Thư viện để biết thêm chi tiết.";
                } else if ("Graduated".equalsIgnoreCase(user.getStatus())) {
                    //Hung: Soạn nội dung chúc mừng và cảnh báo không cho mượn sách nữa khi Tốt nghiệp
                    title = "Cập nhật trạng thái Tốt Nghiệp";
                    content = "Tài khoản của bạn đã được chuyển sang trạng thái Đã Tốt Nghiệp. Bạn sẽ không thể mượn thêm sách nhưng vẫn có thể tra cứu lịch sử.";
                }
                
                if (!title.isEmpty()) {
                    Notification notif = Notification.builder()
                            .user(user)
                            .notificationType("STATUS_UPDATE")
                            .title(title)
                            .content(content)
                            .status("Pending")
                            .createdAt(LocalDateTime.now())
                            .read(false)
                            .build();
                    notificationRepository.save(notif);
                }
            }

            redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật trạng thái thẻ cho " + userId + " thành công!");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy người dùng!");
        }
        return "redirect:/admin/users";
    }

    private String getSafeCellValue(org.apache.poi.ss.usermodel.Row row, int cellIndex) {
        org.apache.poi.ss.usermodel.Cell cell = row.getCell(cellIndex);
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case STRING: return cell.getStringCellValue().trim();
            case NUMERIC:
                if (org.apache.poi.ss.usermodel.DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                }
                return String.valueOf((int) cell.getNumericCellValue());
            case BOOLEAN: return String.valueOf(cell.getBooleanCellValue());
            default: return "";
        }
    }
}