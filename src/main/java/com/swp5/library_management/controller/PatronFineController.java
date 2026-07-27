package com.swp5.library_management.controller;

import com.swp5.library_management.entity.FineInvoice;
import com.swp5.library_management.service.ViolationService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/profile/fines")
@RequiredArgsConstructor
public class PatronFineController {

    private final ViolationService violationService;

    @Value("${app.vietqr.bank-id}")
    private String bankId;

    @Value("${app.vietqr.bank-name}")
    private String bankName;

    @Value("${app.vietqr.account-no}")
    private String accountNo;

    @Value("${app.vietqr.account-name}")
    private String accountName;

    @GetMapping
    public String showFineHistory(
            @RequestParam(required = false, defaultValue = "all") String filterStatus,
            HttpSession session,
            RedirectAttributes redirectAttributes,
            Model model) {

        String loggedInUserId = (String) session.getAttribute("loggedInUserId");
        if (loggedInUserId == null) {
            redirectAttributes.addFlashAttribute("errorMsg", "Vui lòng đăng nhập để xem lịch sử nộp phạt.");
            return "redirect:/login";
        }

        // Lấy tất cả hóa đơn phạt của sinh viên
        List<FineInvoice> allFines = violationService.getAllFineInvoices(loggedInUserId, null);
        
        // Lọc theo trạng thái
        List<FineInvoice> displayFines = allFines;
        if ("unpaid".equalsIgnoreCase(filterStatus)) {
            displayFines = allFines.stream()
                    .filter(f -> f.getPaidStatus() == null || !"PAID".equalsIgnoreCase(f.getPaidStatus()))
                    .collect(Collectors.toList());
        } else if ("paid".equalsIgnoreCase(filterStatus)) {
            displayFines = allFines.stream()
                    .filter(f -> "PAID".equalsIgnoreCase(f.getPaidStatus()))
                    .collect(Collectors.toList());
        }

        model.addAttribute("fines", displayFines);
        model.addAttribute("filterStatus", filterStatus.toLowerCase());

        // Thông tin ngân hàng cho mã QR
        model.addAttribute("vietqrBankId", bankId);
        model.addAttribute("vietqrBankName", bankName);
        model.addAttribute("vietqrAccountNo", accountNo);
        model.addAttribute("vietqrAccountName", accountName);

        // Sidebar active menu
        model.addAttribute("activeNav", "profile");
        model.addAttribute("activeSidebar", "fines");

        return "fines-history";
    }

    @GetMapping("/api/status/{fineId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> checkFineStatus(@PathVariable Integer fineId, HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        String loggedInUserId = (String) session.getAttribute("loggedInUserId");
        
        if (loggedInUserId == null) {
            response.put("success", false);
            response.put("message", "Unauthorized");
            return ResponseEntity.status(401).body(response);
        }

        try {
            FineInvoice fine = violationService.getFineInvoiceById(fineId);
            if (fine != null) {
                // Bảo mật: chỉ cho phép kiểm tra phiếu phạt của chính mình
                if (fine.getPatron() == null || !fine.getPatron().getUserId().equals(loggedInUserId)) {
                    response.put("success", false);
                    response.put("message", "Access denied");
                    return ResponseEntity.status(403).body(response);
                }

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
