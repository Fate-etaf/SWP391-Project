package com.swp5.library_management.controller;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.swp5.library_management.entity.User;
import com.swp5.library_management.repository.CampusRepository;
import com.swp5.library_management.service.TransferService;

import jakarta.servlet.http.HttpSession;


@Controller
@RequestMapping("/librarian/inventory/transfers")
public class TransferController {

    private final CampusRepository campusRepository;
    private final TransferService transferService;

    public TransferController(TransferService transferService, CampusRepository campusRepository) {
        this.transferService = transferService;
        this.campusRepository = campusRepository;
    }

    // Hiển thị giao diện danh sách luân chuyển
    @GetMapping
    public String listTransfers(Model model, HttpSession session) {
        // Kiểm tra quyền: Chỉ cho phép Librarian truy cập
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null || !user.isLibrarian()) {
            return "redirect:/login";
        }

        model.addAttribute("transfers", transferService.getAllTransfers());
        return "inventory/transfers"; 
    }

    // 1. Hiển thị Form tạo lệnh
    @GetMapping("/create")
    public String showCreateForm(Model model, HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null || !user.isLibrarian()) return "redirect:/login";

        // Lấy danh sách các cơ sở để render vào Dropdown
        model.addAttribute("campuses", campusRepository.findAll());
        return "inventory/create-transfer"; 
    }

    // 2. Xử lý Form Submit để tạo lệnh
    @PostMapping("/create")
    public String processCreateTransfer(@RequestParam("toCampusId") Integer toCampusId,
                                        @RequestParam("copyIds") String copyIdsStr,
                                        @RequestParam(value = "note", required = false) String note,
                                        HttpSession session, 
                                        RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null || !user.isLibrarian()) return "redirect:/login";

        try {
            // Lấy cơ sở gốc chính là cơ sở mà Thủ thư đang làm việc
            Integer fromCampusId = user.getCampusId(); 

            // Chuyển chuỗi các mã sách (cách nhau bằng dấu phẩy) thành List<String>
            List<String> copyIds = Arrays.stream(copyIdsStr.split(","))
                                         .map(String::trim)
                                         .filter(s -> !s.isEmpty())
                                         .collect(Collectors.toList());

            // Gọi Service để tạo lệnh
            transferService.createTransfer(fromCampusId, toCampusId, copyIds, user.getUserId(), note);
            
            // Gửi thông báo thành công sang trang danh sách
            redirectAttributes.addFlashAttribute("successMsg", "Tạo lệnh xuất kho thành công!");
        } catch (Exception e) {
            // Gửi thông báo lỗi nếu có sách không hợp lệ
            redirectAttributes.addFlashAttribute("errorMsg", "Lỗi: " + e.getMessage());
        }
        return "redirect:/inventory/transfers";
    }

    // 3. Xem chi tiết một lệnh luân chuyển
    @GetMapping("/{id}")
    public String viewTransferDetail(@PathVariable("id") Integer transferId, Model model, HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null || !user.isLibrarian()) return "redirect:/login";

        model.addAttribute("transfer", transferService.getTransferById(transferId));
        return "inventory/transfer-detail"; 
    }

    // 4. Xác nhận HỦY LỆNH (Trường hợp tạo nhầm)
    @PostMapping("/{id}/cancel")
    public String cancelTransfer(@PathVariable("id") Integer transferId, HttpSession session, RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user != null && user.isLibrarian()) {
            try {
                transferService.cancelTransfer(transferId);
                redirectAttributes.addFlashAttribute("successMsg", "Đã hủy lệnh luân chuyển thành công!");
            } catch (Exception e) {
                redirectAttributes.addFlashAttribute("errorMsg", "Lỗi hủy lệnh: " + e.getMessage());
            }
            return "redirect:/inventory/transfers";
        }
        return "redirect:/login";
    }

    // 5. Cập nhật trạng thái GIAO HÀNG (In Transit)
    @PostMapping("/{id}/ship")
    public String shipTransfer(@PathVariable("id") Integer transferId, HttpSession session, RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user != null && user.isLibrarian()) {
            try {
                transferService.markAsInTransit(transferId);
                redirectAttributes.addFlashAttribute("successMsg", "Lô sách đã được giao cho đơn vị vận chuyển!");
            } catch (Exception e) {
                redirectAttributes.addFlashAttribute("errorMsg", "Lỗi xử lý: " + e.getMessage());
            }
            return "redirect:/inventory/transfers";
        }
        return "redirect:/login";
    }

    // Xử lý hành động "Xác nhận nhập kho"
    @PostMapping("/{id}/confirm")
    public String confirmTransferReceipt(@PathVariable("id") Integer transferId, HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        
        // BẢO MẬT: Kiểm tra xem người đang bấm nút có đăng nhập và có đúng là Thủ thư không
        if (user != null && user.isLibrarian()) {
            transferService.confirmReceipt(transferId, user.getUserId());
            return "redirect:/inventory/transfers";
        }
        
        // Nếu không có quyền hoặc chưa đăng nhập, đá văng về trang login
        return "redirect:/login";
    }
}