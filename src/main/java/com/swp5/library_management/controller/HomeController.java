package com.swp5.library_management.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controller chịu trách nhiệm điều hướng và chuẩn bị dữ liệu hiển thị cho Trang chủ (Home Page)
 * Phục vụ cho Bạn đọc (Sinh viên, Giảng viên) tra cứu và thực hiện các Use Case đặt sách.
 */
@Controller
public class HomeController {

    @GetMapping({"/", "/home"})
    public String home(Model model) {
        // 1. Chuẩn bị dữ liệu thống kê tổng quan (Dành cho phần Metrics)
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalBooks", 12450);
        stats.put("availableCopies", 8940);
        stats.put("activeReaders", 3200);
        stats.put("totalCampuses", 5);
        model.addAttribute("stats", stats);

        // 2. Danh sách các Cơ sở (Campus) để hiển thị trong bộ lọc tìm kiếm
        List<String> campuses = List.of("Hà Nội", "TP. Hồ Chí Minh", "Đà Nẵng", "Cần Thơ", "Quy Nhơn");
        model.addAttribute("campuses", campuses);

        // 3. Chuẩn bị danh mục thể loại sách nổi bật (Phục vụ phần Categories)
        List<Map<String, String>> categories = new ArrayList<>();
        categories.add(Map.of("name", "Công nghệ thông tin", "count", "1,240 đầu sách", "icon", "code", "color", "blue"));
        categories.add(Map.of("name", "Kinh tế & Tài chính", "count", "850 đầu sách", "icon", "chart", "color", "green"));
        categories.add(Map.of("name", "Kỹ năng mềm", "count", "620 đầu sách", "icon", "heart", "color", "amber"));
        categories.add(Map.of("name", "Ngoại ngữ & Nhật Bản học", "count", "980 đầu sách", "icon", "globe", "color", "indigo"));
        categories.add(Map.of("name", "Thiết kế đồ họa", "count", "430 đầu sách", "icon", "paint", "color", "rose"));
        model.addAttribute("categories", categories);

        // 4. Danh sách sách mới nhập kho nổi bật (Featured Books)
        List<Map<String, Object>> featuredBooks = new ArrayList<>();
        featuredBooks.add(Map.of(
            "title", "Clean Code: A Handbook of Agile Software Craftsmanship",
            "author", "Robert C. Martin",
            "subjectCode", "SWE201c",
            "isbn", "978-0132350884",
            "category", "Công nghệ thông tin",
            "isAvailable", true,
            "coverColor", "from-slate-700 to-slate-900"
        ));
        featuredBooks.add(Map.of(
            "title", "Design Patterns: Elements of Reusable Object-Oriented Software",
            "author", "Erich Gamma, Richard Helm",
            "subjectCode", "PRJ301",
            "isbn", "978-0201633610",
            "category", "Công nghệ thông tin",
            "isAvailable", true,
            "coverColor", "from-blue-700 to-indigo-900"
        ));
        featuredBooks.add(Map.of(
            "title", "Kinh tế học vĩ mô (Macroeconomics)",
            "author", "N. Gregory Mankiw",
            "subjectCode", "ECO111",
            "isbn", "978-1305971509",
            "category", "Kinh tế & Tài chính",
            "isAvailable", false, // Giả lập hết sách để sinh viên kích hoạt nút Đặt chỗ (Waitlist)
            "coverColor", "from-emerald-700 to-teal-900"
        ));
        featuredBooks.add(Map.of(
            "title", "Đắc Nhân Tâm (How to Win Friends and Influence People)",
            "author", "Dale Carnegie",
            "subjectCode", "SSG104",
            "isbn", "978-0671027032",
            "category", "Kỹ năng mềm",
            "isAvailable", true,
            "coverColor", "from-red-700 to-rose-900"
        ));
        model.addAttribute("featuredBooks", featuredBooks);

        // Trả về file giao diện templates/home.html
        return "home";
    }
}