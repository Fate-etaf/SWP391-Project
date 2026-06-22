package com.swp5.library_management.controller;

import com.swp5.library_management.entity.User;
import com.swp5.library_management.entity.Role;
import com.swp5.library_management.repository.UserRepository;
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
public class StudentManagementController {

    private final UserRepository userRepository;
    // Bộ gửi mail của hệ thống Spring
    private final JavaMailSender mailSender;

    @GetMapping("/librarian/students")
    public String manageStudents(
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "campusId", required = false) Integer campusId, // Bộ lọc Campus động
            @RequestParam(value = "roleId", required = false) Integer roleId,     // Bộ lọc Chức vụ (Sinh viên/Giảng viên)
            Model model) {
        
        // Truy vấn toàn bộ danh sách, sau đó dùng Stream API để lọc động
        List<User> allUsers = userRepository.findAll();
        java.util.stream.Stream<User> stream = allUsers.stream();
        
        // 1. Lọc theo chuỗi tìm kiếm (Tên hoặc Mã định danh)
        if (search != null && !search.trim().isEmpty()) {
            String cleanSearch = search.trim().toLowerCase();
            stream = stream.filter(u -> (u.getFullName() != null && u.getFullName().toLowerCase().contains(cleanSearch))
                                     || (u.getUserId() != null && u.getUserId().toLowerCase().contains(cleanSearch)));
        }
        
        // 2. Lọc theo trạng thái hoạt động
        if (status != null && !status.isEmpty()) {
            stream = stream.filter(u -> status.equalsIgnoreCase(u.getStatus()));
        }
        
        // 3. Lọc theo Cơ sở (Campus ID)
        if (campusId != null) {
            stream = stream.filter(u -> campusId.equals(u.getCampusId()));
        }
        
        // 4. Lọc theo Chức vụ (Role) - Đã cải tiến để nhận diện cả dữ liệu cũ bị NULL role
        if (roleId != null) {
            if (roleId == 1) {
                // Nếu chọn Sinh viên: Lấy những ai có roleId = 1 HOẶC những ai bị khuyết role (mặc định là sinh viên)
                stream = stream.filter(u -> u.getRole() == null || Integer.valueOf(1).equals(u.getRole().getRoleId()));
            } else {
                // Nếu chọn Giảng viên: Lấy chính xác những ai có roleId = 2
                stream = stream.filter(u -> u.getRole() != null && roleId.equals(u.getRole().getRoleId()));
            }
        }
        
        List<User> filteredStudents = stream.collect(java.util.stream.Collectors.toList());

        // Đẩy dữ liệu ngược ra giao diện để giữ nguyên trạng thái các thẻ select
        model.addAttribute("students", filteredStudents);
        model.addAttribute("currentSearch", search);
        model.addAttribute("currentStatus", status);
        model.addAttribute("currentCampusId", campusId);
        model.addAttribute("currentRoleId", roleId);
        
        return "librarian/students";
    }

    @GetMapping("/librarian/students/import")
    public String showImportPage() {
        return "import-users"; 
    }

    @PostMapping("/librarian/students/import/process")
    public String processExcelUpload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("importType") String importType,
            RedirectAttributes redirectAttributes) {

        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Vui lòng chọn một file Excel trước khi bấm xử lý!");
            return "redirect:/librarian/students/import";
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
                        student.setStatus("Inactive"); 
                        usersToSave.add(student);
                        successCount++;
                    }
                } else {
                    User account = userOpt.orElse(new User());
                    
                    // Chỉ gửi email thông báo nếu đây là tài khoản mới tinh hoặc đang bị khóa
                    if (!userOpt.isPresent() || "Inactive".equalsIgnoreCase(userOpt.get().getStatus())) {
                        if (email != null && !email.trim().isEmpty()) {
                            emailsToNotify.add(email.trim());
                        }
                    }

                    account.setUserId(userId);
                    
                    // Tự động gán danh xưng tương ứng nếu file Excel bị trống cột tên
                    if (fullName.isEmpty()) {
                        account.setFullName("LECTURER".equalsIgnoreCase(importType) ? "Giảng viên FPT" : "Sinh viên FPT");
                    } else {
                        account.setFullName(fullName);
                    }
                    
                    account.setEmail(email);
                    account.setCampusId(campusId);
                    account.setStatus("Active");
                    account.setBorrowingLocked(false);
                    
                    // 🟢 TỰ ĐỘNG KHỞI TẠO VÀ GÁN ROLE ID CHO ĐỐI TƯỢNG XỬ LÝ
                    Role targetRole = new Role();
                    if ("LECTURER".equalsIgnoreCase(importType)) {
                        targetRole.setRoleId(2); // Gán quyền Giảng viên ngầm định
                        account.setRole(targetRole);
                    } else if (!userOpt.isPresent()) {
                        targetRole.setRoleId(1); // Gán quyền Sinh viên cho tài khoản tạo mới
                        account.setRole(targetRole);
                    }

                    if (!userOpt.isPresent()) {
                        account.setPasswordHash("123"); 
                    }
                    
                    usersToSave.add(account);
                    successCount++;
                }
            }

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
                                 ("NEW".equalsIgnoreCase(importType) ? "Sinh viên mới" : "Sinh viên tốt nghiệp");
                                 
                redirectAttributes.addFlashAttribute("successMessage", 
                    "Thành công! Đã xử lý đợt [" + typeText + "]. Đã cập nhật " + successCount + " tài khoản hệ thống và kích hoạt gửi mail thông báo ngầm.");
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy dữ liệu nào cần cập nhật trong file Excel!");
            }

        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi cấu trúc hoặc định dạng file: " + e.getMessage());
        }

        return "redirect:/librarian/students";
    }

    @GetMapping("/librarian/students/toggle-status/{id}")
    public String toggleStudentStatus(
            @org.springframework.web.bind.annotation.PathVariable("id") String userId, 
            RedirectAttributes redirectAttributes) {
        
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User student = userOpt.get();
            if ("Active".equalsIgnoreCase(student.getStatus())) {
                student.setStatus("Inactive");
            } else {
                student.setStatus("Active");
            }
            userRepository.save(student);
            String statusText = "Active".equalsIgnoreCase(student.getStatus()) ? "KÍCH HOẠT" : "KHÓA THẺ";
            redirectAttributes.addFlashAttribute("successMessage", "Đã " + statusText + " tài khoản mã số: " + userId + " thành công!");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy tài khoản người dùng!");
        }
        return "redirect:/librarian/students";
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