package com.swp5.library_management.controller;

import com.swp5.library_management.entity.FineInvoice;
import com.swp5.library_management.service.ViolationService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Controller
@RequestMapping("/librarian/fines")
@RequiredArgsConstructor
public class LibrarianFineController {

    private final ViolationService violationService;

    @org.springframework.beans.factory.annotation.Value("${app.vietqr.bank-id}")
    private String bankId;

    @org.springframework.beans.factory.annotation.Value("${app.vietqr.bank-name}")
    private String bankName;

    @org.springframework.beans.factory.annotation.Value("${app.vietqr.account-no}")
    private String accountNo;

    @org.springframework.beans.factory.annotation.Value("${app.vietqr.account-name}")
    private String accountName;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private boolean isNotLibrarian(HttpSession session) {
        Boolean isLibrarian = (Boolean) session.getAttribute("isLibrarian");
        return isLibrarian == null || !isLibrarian;
    }

    // ────────────────────────────────────────────────────────────
    // GET /librarian/fines – Trang quản lý phiếu phạt
    // ────────────────────────────────────────────────────────────
    @GetMapping
    public String showFineList(@RequestParam(required = false) String patronId,
            @RequestParam(required = false) String paidStatus,
            HttpSession session,
            RedirectAttributes redirectAttributes,
            Model model) {

        if (isNotLibrarian(session)) {
            redirectAttributes.addFlashAttribute("errorMsg", "Quyền truy cập bị từ chối. Vui lòng đăng nhập bằng tài khoản Thủ thư.");
            return "redirect:/login";
        }

        // Mặc định là "Unpaid" nếu không có trạng thái chỉ định để giảm tải DB
        String activeStatus = paidStatus;
        if (activeStatus == null || activeStatus.isBlank()) {
            activeStatus = "Unpaid";
        }

        // Nếu trạng thái là "all", truyền null vào DB để lấy toàn bộ
        String dbStatus = activeStatus;
        if ("all".equalsIgnoreCase(dbStatus)) {
            dbStatus = null;
        }

        List<FineInvoice> fines = violationService.getAllFineInvoices(null, dbStatus);
        model.addAttribute("fines", fines);
        model.addAttribute("patronId", patronId);
        model.addAttribute("paidStatus", activeStatus);

        model.addAttribute("vietqrBankId", bankId);
        model.addAttribute("vietqrBankName", bankName);
        model.addAttribute("vietqrAccountNo", accountNo);
        model.addAttribute("vietqrAccountName", accountName);

        model.addAttribute("activeItem", "fines"); // Add active side bar highlighting token

        return "librarian/fines";
    }

    // ────────────────────────────────────────────────────────────
    // POST /librarian/fines/collect/{fineId} – Thu tiền mặt
    // ────────────────────────────────────────────────────────────
    @PostMapping("/collect/{fineId}")
    public String collectCash(@PathVariable Integer fineId,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        if (isNotLibrarian(session)) {
            redirectAttributes.addFlashAttribute("errorMsg", "Quyền truy cập bị từ chối. Vui lòng đăng nhập bằng tài khoản Thủ thư.");
            return "redirect:/login";
        }

        String librarianId = (String) session.getAttribute("loggedInUserId");
        try {
            violationService.collectFineCash(fineId, librarianId);
            redirectAttributes.addFlashAttribute("successMsg",
                    "Đã xác nhận thu tiền mặt cho phiếu phạt #" + fineId);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/librarian/fines";
    }

    // ────────────────────────────────────────────────────────────
    // POST /librarian/fines/collect-qr/{fineId} – Thanh toán QR
    // ────────────────────────────────────────────────────────────
    @PostMapping("/collect-qr/{fineId}")
    public String collectQR(@PathVariable Integer fineId,
            @RequestParam(required = false) String transactionCode,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        if (isNotLibrarian(session)) {
            redirectAttributes.addFlashAttribute("errorMsg", "Quyền truy cập bị từ chối. Vui lòng đăng nhập bằng tài khoản Thủ thư.");
            return "redirect:/login";
        }

        String librarianId = (String) session.getAttribute("loggedInUserId");
        try {
            violationService.collectFineQR(fineId, librarianId, transactionCode);
            redirectAttributes.addFlashAttribute("successMsg",
                    "Đã xác nhận thanh toán chuyển khoản QR cho phiếu phạt #" + fineId);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/librarian/fines";
    }

    // ────────────────────────────────────────────────────────────
    // GET /librarian/fines/export – Xuất Excel
    // ────────────────────────────────────────────────────────────
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportExcel(@RequestParam(required = false) String patronId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String paidStatus,
            @RequestParam(required = false) String violationType,
            HttpSession session) throws IOException {

        if (isNotLibrarian(session)) {
            return ResponseEntity.status(401).body(null);
        }

        String activeStatus = paidStatus;
        if (activeStatus == null || activeStatus.isBlank()) {
            activeStatus = "Unpaid";
        }
        String dbStatus = activeStatus;
        if ("all".equalsIgnoreCase(dbStatus)) {
            dbStatus = null;
        }

        List<FineInvoice> fines = violationService.getAllFineInvoices(null, dbStatus);

        // Uu tien lay search parameter cua nguoi dung
        String activeSearch = search;
        if ((activeSearch == null || activeSearch.isBlank()) && patronId != null && !patronId.isBlank()) {
            activeSearch = patronId;
        }

        // Loc bang Stream tuong thich 100% voi bo loc phia client-side
        if (activeSearch != null && !activeSearch.isBlank()) {
            final String s = activeSearch.toLowerCase().trim();
            fines = fines.stream().filter(f -> {
                boolean matchPatronId = f.getPatron() != null && f.getPatron().getUserId() != null && f.getPatron().getUserId().toLowerCase().contains(s);
                boolean matchPatronName = f.getPatron() != null && f.getPatron().getFullName() != null && f.getPatron().getFullName().toLowerCase().contains(s);
                boolean matchBookTitle = false;
                boolean matchCopyId = false;
                if (f.getTicketDetail() != null && f.getTicketDetail().getBookCopy() != null) {
                    matchCopyId = f.getTicketDetail().getBookCopy().getCopyId() != null && f.getTicketDetail().getBookCopy().getCopyId().toLowerCase().contains(s);
                    if (f.getTicketDetail().getBookCopy().getBook() != null) {
                        matchBookTitle = f.getTicketDetail().getBookCopy().getBook().getTitle() != null && f.getTicketDetail().getBookCopy().getBook().getTitle().toLowerCase().contains(s);
                    }
                }
                boolean matchTxCode = f.getTransactionCode() != null && f.getTransactionCode().toLowerCase().contains(s);
                boolean matchReason = f.getReason() != null && f.getReason().toLowerCase().contains(s);
                return matchPatronId || matchPatronName || matchBookTitle || matchCopyId || matchTxCode || matchReason;
            }).toList();
        }

        if (paidStatus != null && !paidStatus.isBlank()) {
            final String ps = paidStatus.toLowerCase().trim();
            fines = fines.stream().filter(f -> f.getPaidStatus() != null && f.getPaidStatus().equalsIgnoreCase(ps)).toList();
        }

        if (violationType != null && !violationType.isBlank()) {
            final String vt = violationType.toLowerCase().trim();
            fines = fines.stream().filter(f -> f.getViolationType() != null && f.getViolationType().equalsIgnoreCase(vt)).toList();
        }

        byte[] excelBytes = buildExcel(fines);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDispositionFormData("attachment", "bao-cao-phat.xlsx");

        return ResponseEntity.ok().headers(headers).body(excelBytes);
    }

    // ────────────────────────────────────────────────────────────
    // Private helpers
    // ────────────────────────────────────────────────────────────
    private byte[] buildExcel(List<FineInvoice> fines) throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook();
                ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = wb.createSheet("Phiếu phạt");

            // Header style
            CellStyle headerStyle = wb.createCellStyle();
            Font headerFont = wb.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.ORANGE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);

            // Header row
            String[] cols = { "STT", "Mã SV", "Họ tên", "Tên sách", "Mã bản sao",
                    "Loại vi phạm", "Lý do", "Tiền phạt (VND)", "Còn lại (VND)",
                    "Trạng thái", "N.tạo phạt", "N.thanh toán", "P.thức TT", "Mã GD" };
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < cols.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(cols[i]);
                cell.setCellStyle(headerStyle);
            }

            // Data rows
            int rowNum = 1;
            for (FineInvoice f : fines) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(rowNum - 1);
                row.createCell(1).setCellValue(f.getPatron() != null ? f.getPatron().getUserId() : "");
                row.createCell(2).setCellValue(f.getPatron() != null ? f.getPatron().getFullName() : "");

                String bookTitle = "";
                String copyId = "";
                if (f.getTicketDetail() != null && f.getTicketDetail().getBookCopy() != null) {
                    copyId = f.getTicketDetail().getBookCopy().getCopyId();
                    if (f.getTicketDetail().getBookCopy().getBook() != null) {
                        bookTitle = f.getTicketDetail().getBookCopy().getBook().getTitle();
                    }
                }
                row.createCell(3).setCellValue(bookTitle);
                row.createCell(4).setCellValue(copyId);
                row.createCell(5).setCellValue(f.getViolationType() != null ? f.getViolationType() : "");
                row.createCell(6).setCellValue(f.getReason() != null ? f.getReason() : "");
                row.createCell(7).setCellValue(f.getFineAmount() != null ? f.getFineAmount().doubleValue() : 0);
                row.createCell(8)
                        .setCellValue(f.getRemainingAmount() != null ? f.getRemainingAmount().doubleValue() : 0);
                row.createCell(9).setCellValue(f.getPaidStatus() != null ? f.getPaidStatus() : "");
                row.createCell(10).setCellValue(f.getCreatedAt() != null ? f.getCreatedAt().format(DATE_FMT) : "");
                row.createCell(11).setCellValue(f.getPaidAt() != null ? f.getPaidAt().format(DATE_FMT) : "");
                row.createCell(12).setCellValue(f.getPaymentMethod() != null ? f.getPaymentMethod() : "");
                row.createCell(13).setCellValue(f.getTransactionCode() != null ? f.getTransactionCode() : "");
            }

            // Auto-size columns
            for (int i = 0; i < cols.length; i++) {
                sheet.autoSizeColumn(i);
            }

            wb.write(out);
            return out.toByteArray();
        }
    }

    // ────────────────────────────────────────────────────────────
    // GET /librarian/fines/check-status/{fineId} – Kiểm tra trạng thái thanh toán
    // ────────────────────────────────────────────────────────────
    @GetMapping("/check-status/{fineId}")
    @ResponseBody
    public ResponseEntity<java.util.Map<String, Object>> checkPaymentStatus(@PathVariable Integer fineId, HttpSession session) {
        java.util.Map<String, Object> response = new java.util.HashMap<>();
        if (isNotLibrarian(session)) {
            response.put("success", false);
            response.put("message", "Quyền truy cập bị từ chối. Vui lòng đăng nhập bằng tài khoản Thủ thư.");
            return ResponseEntity.status(401).body(response);
        }
        try {
            FineInvoice fine = violationService.getFineInvoiceById(fineId);
            if (fine != null) {
                boolean isPaid = "Paid".equalsIgnoreCase(fine.getPaidStatus());
                response.put("success", true);
                response.put("paid", isPaid);
                response.put("transactionCode", fine.getTransactionCode());
            } else {
                response.put("success", false);
                response.put("message", "Phiếu phạt không tồn tại");
            }
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        return ResponseEntity.ok(response);
    }
}
