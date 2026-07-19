package com.swp5.library_management.controller;

import com.swp5.library_management.entity.User;
import com.swp5.library_management.entity.Role;
import com.swp5.library_management.repository.UserRepository;
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

@Controller
@RequiredArgsConstructor
public class UserManagementController {

    private final UserRepository userRepository;
    private final JavaMailSender mailSender;
    private final UserStatusService userStatusService;
    private final com.swp5.library_management.repository.BorrowTicketRepository borrowTicketRepository;

    @GetMapping("/admin/users")
    public String manageStudents(
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "campusId", required = false) Integer campusId,
            @RequestParam(value = "tab", defaultValue = "all") String tab,
            Model model) {
        
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
            stream = stream.filter(u -> u.getRole() != null && Integer.valueOf(2).equals(u.getRole().getRoleId()));
        } else if ("librarians".equals(tab)) {
            stream = stream.filter(u -> u.getRole() != null && Integer.valueOf(3).equals(u.getRole().getRoleId()));
        } else if ("admins".equals(tab)) {
            stream = stream.filter(u -> u.getRole() != null && Integer.valueOf(4).equals(u.getRole().getRoleId()));
        } else if ("graduates".equals(tab)) {
            stream = stream.filter(u -> (u.getRole() == null || Integer.valueOf(1).equals(u.getRole().getRoleId())) && ("Inactive".equalsIgnoreCase(u.getStatus()) || "Graduated".equalsIgnoreCase(u.getStatus())));
        } else {
            // default is "students"
            stream = stream.filter(u -> (u.getRole() == null || Integer.valueOf(1).equals(u.getRole().getRoleId())) && !"Inactive".equalsIgnoreCase(u.getStatus()) && !"Graduated".equalsIgnoreCase(u.getStatus()));
        }
        
        List<User> filteredStudents = stream.collect(java.util.stream.Collectors.toList());
        userStatusService.enrichStatuses(filteredStudents);

        model.addAttribute("students", filteredStudents);
        model.addAttribute("currentSearch", search);
        model.addAttribute("currentCampusId", campusId);
        model.addAttribute("currentTab", tab);
        
        return "admin/users";
    }

    @PostMapping("/admin/users/add-manual")
    public String addManualStudent(
            @RequestParam("userId") String userId,
            @RequestParam("fullName") String fullName,
            @RequestParam(value = "email", required = false) String email,
            @RequestParam("campusId") Integer campusId,
            @RequestParam(value = "roleId", defaultValue = "1") Integer roleId,
            @RequestParam(value = "status", defaultValue = "Active") String status,
            RedirectAttributes redirectAttributes) {
        
        Optional<User> existingUser = userRepository.findById(userId);
        if (existingUser.isPresent()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Mã người dùng đã tồn tại trong hệ thống!");
            return "redirect:/admin/users";
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
        
        newUser.setPasswordHash("123");
        
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
                    message.setText("Xin chào bạn,\n\nTài khoản thư viện số FLMS của bạn trên hệ thống đã được kích hoạt thành công bởi Ban quản trị.\nBây giờ bạn đã có thể truy cập hệ thống và thực hiện mượn trả tài liệu.\n\nTrân trọng,\nBan quản lý thư viện Đại học FPT.");
                    mailSender.send(message);
                } catch (Exception e) {
                    System.out.println("Lỗi gửi mail đến: " + finalEmail + " -> " + e.getMessage());
                }
            }).start();
        }

        redirectAttributes.addFlashAttribute("successMessage", "Thêm mới thủ công tài khoản: " + userId.trim().toUpperCase() + " thành công và đã gửi mail thông báo!");
        
        return "redirect:/admin/users";
    }

    @PostMapping("/admin/users/edit")
    public String editStudent(
            @RequestParam("userId") String userId,
            @RequestParam("fullName") String fullName,
            @RequestParam("email") String email,
            @RequestParam("campusId") Integer campusId,
            @RequestParam("roleId") Integer roleId,
            @RequestParam("status") String status,
            RedirectAttributes redirectAttributes) {
        
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setFullName(fullName.trim());
            user.setEmail(email.trim());
            user.setCampusId(campusId);
            user.setStatus(status);
            
            Role userRole = new Role();
            userRole.setRoleId(roleId);
            user.setRole(userRole);
            
            userRepository.save(user);
            redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật thông tin tài khoản: " + userId + " thành công!");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy người dùng để cập nhật!");
        }
        
        return "redirect:/admin/users";
    }

    @PostMapping("/admin/users/delete")
    public String deleteStudent(
            @RequestParam("userId") String userId,
            RedirectAttributes redirectAttributes) {
        
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            int activeBorrowCount = borrowTicketRepository.countByPatronUserId(userId);
            if (activeBorrowCount > 0) {
                redirectAttributes.addFlashAttribute("errorMessage", "Không thể xóa! Sinh viên/Giảng viên này đang có lịch sử mượn trả tài liệu. Vui lòng sử dụng tính năng Khóa Thẻ.");
            } else {
                userRepository.deleteById(userId);
                redirectAttributes.addFlashAttribute("successMessage", "Đã xóa hoàn toàn tài khoản: " + userId + " khỏi hệ thống!");
            }
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy người dùng để xóa!");
        }
        
        return "redirect:/admin/users";
    }

    @GetMapping("/admin/users/import")
    public String showImportPage() {
        return "admin/users-import"; 
    }

    @PostMapping("/admin/users/import/process")
    public String processExcelUpload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("importType") String importType,
            RedirectAttributes redirectAttributes) {

        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Vui lòng chọn một file Excel trước khi bấm xử lý!");
            return "redirect:/admin/users/import";
        }

        List<User> usersToSave = new ArrayList<>();
        List<String> emailsToNotify = new ArrayList<>(); // Lưu email tài khoản mới phục vụ gửi mail ngầm
        int successCount = 0;

        try (InputStream is = file.getInputStream();
             org.apache.poi.ss.usermodel.Workbook workbook = org.apache.poi.ss.usermodel.WorkbookFactory.create(is)) {

            org.apache.poi.ss.usermodel.Sheet sheet = workbook.getSheetAt(0);
            int lastRow = sheet.getLastRowNum();

            for (int i = 1; i <= lastRow; i++) {
                org.apache.poi.ss.usermodel.Row row = sheet.getRow(i);
                if (row == null) continue;

                String userId = getSafeCellValue(row, 0);
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
                        student.setStatus("Graduated"); 
                        usersToSave.add(student);
                        successCount++;
                    }
                } else {
                    if (userOpt.isPresent()) {
                        continue; // Bỏ qua trùng lặp theo đúng chuẩn Use Case E1
                    }
                    
                    User account = new User();
                    
                    if (email != null && !email.trim().isEmpty()) {
                        emailsToNotify.add(email.trim());
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
                        targetRole.setRoleId(2); 
                        account.setRole(targetRole);
                    } else {
                        targetRole.setRoleId(1); 
                        account.setRole(targetRole);
                    }

                    account.setPasswordHash("123"); 
                    
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
                                message.setText("Xin chào bạn,\n\nTài khoản thư viện số FLMS của bạn trên hệ thống đã được kích hoạt thành công bởi Ban quản trị.\nBây giờ bạn đã có thể truy cập hệ thống và thực hiện mượn trả tài liệu.\n\nTrân trọng,\nBan quản lý thư viện Đại học FPT.");
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

    @GetMapping("/admin/users/toggle-lock/{id}")
    public String toggleUserLock(
            @org.springframework.web.bind.annotation.PathVariable("id") String userId, 
            RedirectAttributes redirectAttributes) {
        
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User student = userOpt.get();
            boolean isLocked = student.getBorrowingLocked() != null ? student.getBorrowingLocked() : false;
            student.setBorrowingLocked(!isLocked);
            userRepository.save(student);
            String statusText = isLocked ? "MỞ KHÓA" : "KHÓA THẺ";
            redirectAttributes.addFlashAttribute("successMessage", "Đã " + statusText + " tài khoản mã số: " + userId + " thành công!");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy tài khoản người dùng!");
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