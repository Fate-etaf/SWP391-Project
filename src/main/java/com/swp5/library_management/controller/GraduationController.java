package com.swp5.library_management.controller;

import com.swp5.library_management.dto.GraduationCheckDTO;
import com.swp5.library_management.service.GraduationEmailService;
import com.swp5.library_management.service.GraduationService;
import com.swp5.library_management.repository.NotificationRepository;
import com.swp5.library_management.repository.UserRepository;
import com.swp5.library_management.entity.Notification;
import com.swp5.library_management.entity.User;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
@RequestMapping("/graduation")
@Slf4j
public class GraduationController {

    private final GraduationService graduationService;
    private final GraduationEmailService graduationEmailService;
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    private boolean isNotLibrarian(HttpSession session) {
        Boolean isLibrarian = (Boolean) session.getAttribute("isLibrarian");
        return isLibrarian == null || !isLibrarian;
    }

    @GetMapping("/check")
    public String showCheckPage(HttpSession session, Model model) {
        if (isNotLibrarian(session)) {
            return "redirect:/login";
        }

        @SuppressWarnings("unchecked")
        List<GraduationCheckDTO> clearedList =
                (List<GraduationCheckDTO>) session.getAttribute("graduationClearedList");
        @SuppressWarnings("unchecked")
        List<GraduationCheckDTO> notClearedList =
                (List<GraduationCheckDTO>) session.getAttribute("graduationNotClearedList");
        String fileName = (String) session.getAttribute("graduationFileName");

        if (clearedList != null && notClearedList != null) {
            model.addAttribute("clearedList", clearedList);
            model.addAttribute("notClearedList", notClearedList);
            model.addAttribute("fileName", fileName);
            model.addAttribute("hasResults", true);

            long totalStudentsCount = calculateDistinctStudents(clearedList, notClearedList);
            long clearedStudentsCount = clearedList.size();
            long violationStudentsCount = calculateDistinctViolatingStudents(notClearedList);

            model.addAttribute("totalStudentsCount", totalStudentsCount);
            model.addAttribute("clearedStudentsCount", clearedStudentsCount);
            model.addAttribute("violationStudentsCount", violationStudentsCount);
        }
        model.addAttribute("activeItem", "graduationCheck");
        return "graduation/check";
    }

    @PostMapping("/check")
    public String processExcel(
            @RequestParam("file") MultipartFile file,
            RedirectAttributes redirectAttributes,
            HttpSession session,
            Model model) {

        if (isNotLibrarian(session)) {
            return "redirect:/login";
        }
        model.addAttribute("activeItem", "graduationCheck");

        // Validate file
        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Vui lòng chọn file Excel để tải lên.");
            return "redirect:/graduation/check";
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.endsWith(".xlsx")) {
            redirectAttributes.addFlashAttribute("error", "Chỉ hỗ trợ file Excel định dạng .xlsx");
            return "redirect:/graduation/check";
        }

        try {
            List<GraduationCheckDTO> allResults = graduationService.checkFromExcel(file);

            List<GraduationCheckDTO> clearedList = allResults.stream()
                    .filter(GraduationCheckDTO::isCleared)
                    .collect(Collectors.toList());

            List<GraduationCheckDTO> notClearedList = allResults.stream()
                    .filter(dto -> !dto.isCleared())
                    .collect(Collectors.toList());

            // Lưu vào session để giữ trạng thái hiển thị và dùng khi gửi email
            session.setAttribute("graduationClearedList", clearedList);
            session.setAttribute("graduationNotClearedList", notClearedList);
            session.setAttribute("graduationFileName", originalFilename);

            model.addAttribute("clearedList", clearedList);
            model.addAttribute("notClearedList", notClearedList);
            model.addAttribute("fileName", originalFilename);
            model.addAttribute("hasResults", true);

            long totalStudentsCount = calculateDistinctStudents(clearedList, notClearedList);
            long clearedStudentsCount = clearedList.size();
            long violationStudentsCount = calculateDistinctViolatingStudents(notClearedList);

            model.addAttribute("totalStudentsCount", totalStudentsCount);
            model.addAttribute("clearedStudentsCount", clearedStudentsCount);
            model.addAttribute("violationStudentsCount", violationStudentsCount);

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi xử lý file: " + e.getMessage());
            return "redirect:/graduation/check";
        }

        return "graduation/check";
    }

    @GetMapping("/clear-results")
    public String clearResults(HttpSession session) {
        if (isNotLibrarian(session)) {
            return "redirect:/login";
        }
        session.removeAttribute("graduationClearedList");
        session.removeAttribute("graduationNotClearedList");
        session.removeAttribute("graduationFileName");
        return "redirect:/graduation/check";
    }

    // ── Gửi email cho 1 sinh viên cụ thể ──────────────────────────────────
    @PostMapping("/send-email")
    @ResponseBody
    public ResponseEntity<?> sendSingleEmail(
            @RequestParam("studentId") String studentId,
            HttpSession session) {

        if (isNotLibrarian(session)) {
            return ResponseEntity.status(401).body(Map.of("error", "Quyền truy cập bị từ chối."));
        }

        @SuppressWarnings("unchecked")
        List<GraduationCheckDTO> notClearedList =
                (List<GraduationCheckDTO>) session.getAttribute("graduationNotClearedList");

        if (notClearedList == null || notClearedList.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Không có dữ liệu vi phạm. Vui lòng import lại file Excel."));
        }

        // Lọc tất cả vi phạm của student này
        List<GraduationCheckDTO> studentViolations = notClearedList.stream()
                .filter(dto -> studentId.equals(dto.getStudentId()))
                .collect(Collectors.toList());

        if (studentViolations.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Không tìm thấy vi phạm của sinh viên " + studentId));
        }

        try {
            sendViolationEmail(studentViolations);
            saveGraduationNotification(studentId, studentViolations, "Sent", null);
            return ResponseEntity.ok(Map.of("success", true, "message", "Đã gửi email thông báo vi phạm cho sinh viên " + studentId + " thành công!"));
        } catch (Exception e) {
            saveGraduationNotification(studentId, studentViolations, "Failed", e.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", "Có lỗi xảy ra khi gửi email: " + e.getMessage()));
        }
    }

    // ── Gửi email cho TẤT CẢ sinh viên vi phạm ───────────────────────────
    @PostMapping("/send-all-emails")
    @ResponseBody
    public ResponseEntity<?> sendAllEmails(HttpSession session) {
        if (isNotLibrarian(session)) {
            return ResponseEntity.status(401).body(Map.of("error", "Quyền truy cập bị từ chối."));
        }

        @SuppressWarnings("unchecked")
        List<GraduationCheckDTO> notClearedList =
                (List<GraduationCheckDTO>) session.getAttribute("graduationNotClearedList");

        if (notClearedList == null || notClearedList.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Không có dữ liệu vi phạm. Vui lòng import lại file Excel."));
        }

        // Nhóm vi phạm theo mã sinh viên
        Map<String, List<GraduationCheckDTO>> groupedByStudent = notClearedList.stream()
                .filter(dto -> dto.getEmail() != null && !dto.getEmail().isBlank())
                .collect(Collectors.groupingBy(GraduationCheckDTO::getStudentId));

        if (groupedByStudent.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Không có sinh viên vi phạm nào có email trên hệ thống."));
        }

        int successCount = 0;
        List<String> failedList = new java.util.ArrayList<>();

        for (Map.Entry<String, List<GraduationCheckDTO>> entry : groupedByStudent.entrySet()) {
            String studentId = entry.getKey();
            List<GraduationCheckDTO> studentViolations = entry.getValue();

            try {
                sendViolationEmail(studentViolations);
                saveGraduationNotification(studentId, studentViolations, "Sent", null);
                successCount++;
            } catch (Exception e) {
                log.error("[GRADUATION BULK MAIL FAILED] Student: {} | Error: {}", studentId, e.getMessage(), e);
                saveGraduationNotification(studentId, studentViolations, "Failed", e.getMessage());
                failedList.add(studentId + " (" + e.getMessage() + ")");
            }
        }

        if (failedList.isEmpty()) {
            return ResponseEntity.ok(Map.of("success", true, "message", "Đã gửi email thông báo vi phạm cho tất cả " + successCount + " sinh viên thành công!"));
        } else {
            String failMsg = "Đã gửi thành công " + successCount + "/" + (successCount + failedList.size()) + " sinh viên. "
                    + "Thất bại: " + String.join(", ", failedList);
            return ResponseEntity.ok(Map.of("success", true, "message", failMsg));
        }
    }

    @GetMapping("/export-cleared")
    public void exportCleared(HttpSession session, HttpServletResponse response) throws IOException {
        if (isNotLibrarian(session)) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Quyền truy cập bị từ chối.");
            return;
        }
        @SuppressWarnings("unchecked")
        List<GraduationCheckDTO> list = (List<GraduationCheckDTO>) session.getAttribute("graduationClearedList");
        if (list == null) {
            list = java.util.Collections.emptyList();
        }

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=danh_sach_du_dieu_kien_tot_nghiep.xlsx");

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Đủ điều kiện");

            // Title Row
            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("DANH SÁCH SINH VIÊN ĐỦ ĐIỀU KIỆN TỐT NGHIỆP");
            CellStyle titleStyle = workbook.createCellStyle();
            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);
            titleStyle.setFont(titleFont);
            titleCell.setCellStyle(titleStyle);

            // Header Row
            Row headerRow = sheet.createRow(2);
            headerRow.createCell(0).setCellValue("STT");
            headerRow.createCell(1).setCellValue("Mã sinh viên");
            headerRow.createCell(2).setCellValue("Họ tên");

            // Header styling
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            for (int i = 0; i < 3; i++) {
                headerRow.getCell(i).setCellStyle(headerStyle);
            }

            // Data rows styling
            CellStyle borderStyle = workbook.createCellStyle();
            borderStyle.setBorderBottom(BorderStyle.THIN);
            borderStyle.setBorderTop(BorderStyle.THIN);
            borderStyle.setBorderLeft(BorderStyle.THIN);
            borderStyle.setBorderRight(BorderStyle.THIN);

            int rowIdx = 3;
            int stt = 1;
            for (GraduationCheckDTO dto : list) {
                Row row = sheet.createRow(rowIdx++);
                
                Cell c0 = row.createCell(0);
                c0.setCellValue(stt++);
                c0.setCellStyle(borderStyle);

                Cell c1 = row.createCell(1);
                c1.setCellValue(dto.getStudentId());
                c1.setCellStyle(borderStyle);

                Cell c2 = row.createCell(2);
                c2.setCellValue(dto.getFullName());
                c2.setCellStyle(borderStyle);
            }

            // Auto-size columns
            for (int i = 0; i < 3; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(response.getOutputStream());
        }
    }

    @GetMapping("/export-violations")
    public void exportViolations(HttpSession session, HttpServletResponse response) throws IOException {
        if (isNotLibrarian(session)) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Quyền truy cập bị từ chối.");
            return;
        }
        @SuppressWarnings("unchecked")
        List<GraduationCheckDTO> list = (List<GraduationCheckDTO>) session.getAttribute("graduationNotClearedList");
        if (list == null) {
            list = java.util.Collections.emptyList();
        }

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=danh_sach_vi_pham_nghia_vu.xlsx");

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Danh sách vi phạm");

            // Title Row
            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("DANH SÁCH SINH VIÊN VI PHẠM NGHĨA VỤ THƯ VIỆN");
            CellStyle titleStyle = workbook.createCellStyle();
            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);
            titleStyle.setFont(titleFont);
            titleCell.setCellStyle(titleStyle);

            // Header Row
            Row headerRow = sheet.createRow(2);
            headerRow.createCell(0).setCellValue("STT");
            headerRow.createCell(1).setCellValue("Mã sinh viên");
            headerRow.createCell(2).setCellValue("Họ tên");
            headerRow.createCell(3).setCellValue("Mã cuốn sách (CopyID)");
            headerRow.createCell(4).setCellValue("Tên sách");
            headerRow.createCell(5).setCellValue("Lý do");
            headerRow.createCell(6).setCellValue("Số tiền còn nợ");

            // Header styling
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.RED.getIndex());
            // Wait, RED front color might be hard to read, let's use GREY_25_PERCENT or light red text
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            for (int i = 0; i < 7; i++) {
                headerRow.getCell(i).setCellStyle(headerStyle);
            }

            // Cell border styling
            CellStyle borderStyle = workbook.createCellStyle();
            borderStyle.setBorderBottom(BorderStyle.THIN);
            borderStyle.setBorderTop(BorderStyle.THIN);
            borderStyle.setBorderLeft(BorderStyle.THIN);
            borderStyle.setBorderRight(BorderStyle.THIN);

            // Currency formatting
            CellStyle currencyStyle = workbook.createCellStyle();
            currencyStyle.setBorderBottom(BorderStyle.THIN);
            currencyStyle.setBorderTop(BorderStyle.THIN);
            currencyStyle.setBorderLeft(BorderStyle.THIN);
            currencyStyle.setBorderRight(BorderStyle.THIN);
            DataFormat format = workbook.createDataFormat();
            currencyStyle.setDataFormat(format.getFormat("#,##0"));

            int rowIdx = 3;
            int stt = 1;
            for (GraduationCheckDTO dto : list) {
                Row row = sheet.createRow(rowIdx++);

                Cell c0 = row.createCell(0);
                c0.setCellValue(stt++);
                c0.setCellStyle(borderStyle);

                Cell c1 = row.createCell(1);
                c1.setCellValue(dto.getStudentId());
                c1.setCellStyle(borderStyle);

                Cell c2 = row.createCell(2);
                c2.setCellValue(dto.getFullName());
                c2.setCellStyle(borderStyle);

                Cell c3 = row.createCell(3);
                c3.setCellValue(dto.getCopyId());
                c3.setCellStyle(borderStyle);

                Cell c4 = row.createCell(4);
                c4.setCellValue(dto.getBookTitle());
                c4.setCellStyle(borderStyle);

                Cell c5 = row.createCell(5);
                c5.setCellValue(dto.getReason());
                c5.setCellStyle(borderStyle);

                Cell c6 = row.createCell(6);
                if (dto.getRemainingAmount() != null) {
                    c6.setCellValue(dto.getRemainingAmount().doubleValue());
                    c6.setCellStyle(currencyStyle);
                } else {
                    c6.setCellValue("—");
                    c6.setCellStyle(borderStyle);
                }
            }

            // Auto-size columns
            for (int i = 0; i < 7; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(response.getOutputStream());
        }
    }

    // ── Helper: Build HTML rows rồi gọi EmailService ──────────────────────
    private void sendViolationEmail(List<GraduationCheckDTO> violations) {
        if (violations.isEmpty()) return;

        GraduationCheckDTO first = violations.get(0);
        String email = first.getEmail();
        if (email == null || email.isBlank()) return;

        // Build bảng HTML chi tiết vi phạm
        StringBuilder rowsHtml = new StringBuilder();
        for (GraduationCheckDTO v : violations) {
            rowsHtml.append("<tr>")
                    .append("<td style=\"padding:10px; border:1px solid #ddd;\">").append(v.getCopyId()).append("</td>")
                    .append("<td style=\"padding:10px; border:1px solid #ddd;\">").append(v.getBookTitle()).append("</td>")
                    .append("<td style=\"padding:10px; border:1px solid #ddd; color:#c62828; font-weight:bold;\">").append(v.getReason()).append("</td>")
                    .append("</tr>");
        }

        graduationEmailService.sendViolationNotification(
                email,
                first.getFullName(),
                first.getStudentId(),
                rowsHtml.toString()
        );
    }

    private void saveGraduationNotification(String studentId, List<GraduationCheckDTO> violations, String status, String errorMessage) {
        try {
            userRepository.findById(studentId).ifPresent(user -> {
                // Build bảng HTML chi tiết vi phạm
                StringBuilder rowsHtml = new StringBuilder();
                for (GraduationCheckDTO v : violations) {
                    rowsHtml.append("<tr>")
                            .append("<td style=\"padding:10px; border:1px solid #ddd;\">").append(v.getCopyId()).append("</td>")
                            .append("<td style=\"padding:10px; border:1px solid #ddd;\">").append(v.getBookTitle()).append("</td>")
                            .append("<td style=\"padding:10px; border:1px solid #ddd; color:#c62828; font-weight:bold;\">").append(v.getReason()).append("</td>")
                            .append("</tr>");
                }

                String body = buildEmailBodyForLog(user.getFullName(), studentId, rowsHtml.toString(), errorMessage);

                Notification notification = Notification.builder()
                        .user(user)
                        .notificationType("GRADUATION_VIOLATION")
                        .title("[Thư viện FPT] Thông báo nghĩa vụ thư viện chưa hoàn thành")
                        .content(body)
                        .status(status)
                        .sentAt(status.equals("Sent") ? LocalDateTime.now() : null)
                        .createdAt(LocalDateTime.now())
                        .read(false)
                        .build();

                notificationRepository.save(notification);
            });
        } catch (Exception e) {
            log.error("Lỗi khi lưu Notification log cho sinh viên {}: {}", studentId, e.getMessage(), e);
        }
    }

    private String buildEmailBodyForLog(String patronName, String studentId, String violationRows, String errorMessage) {
        String errorSection = (errorMessage != null && !errorMessage.isBlank())
                ? "<div style=\"background:#ffebee; border-left:4px solid #f44336; padding:12px; border-radius:4px; margin-bottom:16px;\">" +
                  "<p style=\"margin:0; color:#c62828;\">❌ <strong>Lỗi gửi email:</strong> " + errorMessage + "</p></div>"
                : "";

        return """
                <html><body style="font-family: Arial, sans-serif; color: #333;">
                  <div style="max-width:650px; margin:auto; border:1px solid #e0e0e0; border-radius:8px; overflow:hidden;">
                    <div style="background:#c62828; padding:20px; text-align:center;">
                       <h2 style="color:#fff; margin:0;">⚠️ Thông báo nghĩa vụ thư viện</h2>
                    </div>
                    <div style="padding:24px;">
                      %s
                      <p>Xin chào <strong>%s</strong> (MSSV: <strong>%s</strong>),</p>
                      <p>Hệ thống phát hiện bạn <strong style="color:#c62828;">chưa hoàn thành</strong> nghĩa vụ thư viện. Chi tiết vi phạm như sau:</p>
                      <table style="width:100%%; border-collapse:collapse; margin:16px 0;">
                        <tr style="background:#f5f5f5;">
                          <th style="padding:10px; border:1px solid #ddd; text-align:left;">Mã bản sao</th>
                          <th style="padding:10px; border:1px solid #ddd; text-align:left;">Tên sách</th>
                          <th style="padding:10px; border:1px solid #ddd; text-align:left;">Lý do</th>
                        </tr>
                        %s
                      </table>
                      <div style="background:#fff3e0; border-left:4px solid #ff9800; padding:12px; border-radius:4px;">
                        <p style="margin:0;">⚠️ <strong>Lưu ý:</strong> Bạn cần hoàn thành các nghĩa vụ trên trước khi có thể xác nhận đủ điều kiện tốt nghiệp. Vui lòng liên hệ thư viện để giải quyết sớm nhất.</p>
                      </div>
                      <p style="margin-top:20px;">Trân trọng,<br><strong>Hệ thống Thư viện FPT University</strong></p>
                    </div>
                  </div>
                </body></html>
                """.formatted(errorSection, patronName, studentId, violationRows);
    }

    private long calculateDistinctStudents(List<GraduationCheckDTO> cleared, List<GraduationCheckDTO> notCleared) {
        Set<String> studentIds = new HashSet<>();
        if (cleared != null) {
            for (GraduationCheckDTO dto : cleared) {
                if (dto.getStudentId() != null) {
                    studentIds.add(dto.getStudentId());
                }
            }
        }
        if (notCleared != null) {
            for (GraduationCheckDTO dto : notCleared) {
                if (dto.getStudentId() != null) {
                    studentIds.add(dto.getStudentId());
                }
            }
        }
        return studentIds.size();
    }

    private long calculateDistinctViolatingStudents(List<GraduationCheckDTO> notCleared) {
        Set<String> studentIds = new HashSet<>();
        if (notCleared != null) {
            for (GraduationCheckDTO dto : notCleared) {
                if (dto.getStudentId() != null) {
                    studentIds.add(dto.getStudentId());
                }
            }
        }
        return studentIds.size();
    }
}
