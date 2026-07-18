package com.swp5.library_management.controller;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.core.io.InputStreamResource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.swp5.library_management.dto.ReportSummaryDTO;
import com.swp5.library_management.entity.User;
import com.swp5.library_management.repository.UserRepository;
import com.swp5.library_management.service.ReportService;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/librarian/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;
    private final UserRepository userRepository;

    private boolean isNotLibrarian(HttpSession session) {
        Boolean isLibrarian = (Boolean) session.getAttribute("isLibrarian");
        return isLibrarian == null || !isLibrarian;
    }

    // 1. Giao diện báo cáo trực quan
    @GetMapping
    public String viewReports(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            HttpSession session, Model model) {
        
        if (isNotLibrarian(session)) return "redirect:/login";

        // Mặc định lấy dữ liệu của tháng hiện tại nếu không chọn ngày
        if (fromDate == null) fromDate = LocalDate.now().withDayOfMonth(1);
        if (toDate == null) toDate = LocalDate.now();

        LocalDateTime start = fromDate.atStartOfDay();
        LocalDateTime end = toDate.atTime(23, 59, 59);

        // Lấy CampusID của thủ thư đang đăng nhập
        String loggedInUserId = (String) session.getAttribute("loggedInUserId");
        User librarian = userRepository.findById(loggedInUserId).orElse(null);
        Integer campusId = (librarian != null) ? librarian.getCampusId() : null;

        if (campusId != null) {
            ReportSummaryDTO reportData = reportService.getCampusReportSummary(campusId, start, end);
            model.addAttribute("reportData", reportData);
        }

        model.addAttribute("fromDate", fromDate);
        model.addAttribute("toDate", toDate);

        // Return view (Bạn sẽ tạo file reports.html tương tự dashboard.html)
        return "librarian/reports"; 
    }

    // 2. Export ra file Excel
    @GetMapping("/export")
    public ResponseEntity<InputStreamResource> exportExcel(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            HttpSession session) throws IOException {

        if (isNotLibrarian(session)) return ResponseEntity.status(403).build();

        if (fromDate == null) fromDate = LocalDate.now().withDayOfMonth(1);
        if (toDate == null) toDate = LocalDate.now();

        LocalDateTime start = fromDate.atStartOfDay();
        LocalDateTime end = toDate.atTime(23, 59, 59);

        String loggedInUserId = (String) session.getAttribute("loggedInUserId");
        User librarian = userRepository.findById(loggedInUserId).orElse(null);
        Integer campusId = (librarian != null) ? librarian.getCampusId() : null;

        ByteArrayInputStream in = reportService.generateExcelReport(campusId, start, end);

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=Library_Report_" + fromDate + "_to_" + toDate + ".xlsx");

        return ResponseEntity
                .ok()
                .headers(headers)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(new InputStreamResource(in));
    }
}