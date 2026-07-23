package com.swp5.library_management.controller;

import java.io.IOException;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;

import com.swp5.library_management.dto.ReportDataDTO;
import com.swp5.library_management.dto.ReportFilterDTO;
import com.swp5.library_management.entity.User;
import com.swp5.library_management.entity.Major;
import com.swp5.library_management.entity.Subject;
import com.swp5.library_management.repository.CampusRepository;
import com.swp5.library_management.repository.MajorRepository;
import com.swp5.library_management.repository.SubjectRepository;
import com.swp5.library_management.service.ReportService;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/librarian/reports")
public class ReportController {

    private final ReportService reportService;
    private final CampusRepository campusRepository;
    private final MajorRepository majorRepository;
    private final SubjectRepository subjectRepository;

    public ReportController(ReportService reportService, CampusRepository campusRepository,
                            MajorRepository majorRepository, SubjectRepository subjectRepository) {
        this.reportService = reportService;
        this.campusRepository = campusRepository;
        this.majorRepository = majorRepository;
        this.subjectRepository = subjectRepository;
    }

    // Xử lý toàn bộ logic Load lần đầu & Apply Filter thông qua Form GET
    @GetMapping
    public String viewTransactionReport(@ModelAttribute("filter") ReportFilterDTO filter,
            HttpSession session,
            Model model) {
        // 1. Kiểm tra quyền truy cập
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null || !user.isLibrarian()) {
            return "redirect:/login";
        }

        // 2. Bảo mật tham số (Role-based Param Injection)
        // Nếu là Thủ thư chi nhánh, ép cứng CampusID. Admin thì được quyền xem và lọc.
        if (user.getPrimaryRole().isPresent() && user.getPrimaryRole().get().getRoleName().equalsIgnoreCase("LIBRARIAN")) {
            filter.setCampusId(user.getCampusId());
        }

        // Thiết lập ngày mặc định nếu mới vào trang lần đầu
        filter.initDefaultDatesIfNull();

        // 3. Gọi Service xử lý dữ liệu động dựa theo reportType
        ReportDataDTO<?> reportData = reportService.generateReport(filter);

        // 4. Đẩy dữ liệu ra View
        model.addAttribute("data", reportData);
        model.addAttribute("campuses", campusRepository.findAll()); // Dành cho Admin chọn

        // Nạp danh sách Major và Subject cho tính năng Cascading Dropdown
        List<Major> majors = majorRepository.findAll();
        model.addAttribute("majors", majors);
        
        Map<Integer, List<Subject>> majorSubjectMap = new HashMap<>();
        for (Major major : majors) {
            majorSubjectMap.put(major.getMajorId(), new ArrayList<>(major.getSubjects()));
        }
        model.addAttribute("majorSubjectMap", majorSubjectMap);

        // Trả về giao diện Thymeleaf (sẽ được xây dựng sau)
        return "librarian/reports";
    }

    // Nút Export Excel sẽ gọi vào endpoint này (bỏ qua Pagination)
    @GetMapping("/export")
    public void exportReportToExcel(@ModelAttribute("filter") ReportFilterDTO filter,
            HttpSession session,
            HttpServletResponse response) throws IOException {

        // 1. Kiểm tra quyền truy cập (Quan trọng để tránh ai đó có link gọi trực tiếp)
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null || !user.isLibrarian()) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Bạn không có quyền tải báo cáo này.");
            return;
        }

        // 2. Bảo mật tham số Campus
        if (user.getPrimaryRole().isPresent() && user.getPrimaryRole().get().getRoleName().equalsIgnoreCase("LIBRARIAN")) {
            filter.setCampusId(user.getCampusId());
        }

        filter.initDefaultDatesIfNull();

        // 3. Gọi Service để ghi đè dữ liệu thẳng vào response (Trình duyệt sẽ tự động
        // tải file)
        reportService.exportTransactionReportToExcel(filter, response);
    }
}