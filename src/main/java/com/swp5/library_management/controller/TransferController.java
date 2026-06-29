package com.swp5.library_management.controller;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.swp5.library_management.entity.BookCopy;

import com.swp5.library_management.repository.BookCopyRepository;
import com.swp5.library_management.repository.WaitlistRepository;
import com.swp5.library_management.dto.WaitlistHotspotDTO;

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

    private final TransferService transferService;
    private final CampusRepository campusRepository;
    private final WaitlistRepository waitlistRepository;
    private final BookCopyRepository bookCopyRepository;

    public TransferController(TransferService transferService, CampusRepository campusRepository, WaitlistRepository waitlistRepository, BookCopyRepository bookCopyRepository) {
        this.transferService = transferService;
        this.campusRepository = campusRepository;
        this.waitlistRepository = waitlistRepository;
        this.bookCopyRepository = bookCopyRepository;
    }

    // 1. Hiển thị Giao diện Kanban Board (Nơi xử lý chính)
    @GetMapping
    public String viewKanbanBoard(Model model, HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null || !user.isLibrarian())
            return "redirect:/login";

        Integer campusId = user.getCampusId();
        
        // Outbound: Yêu cầu từ cơ sở khác xin sách của mình (Pending, Accepted, Rejected)
        List<com.swp5.library_management.entity.TransferRequest> outbound = transferService.getWorkingOutboundRequests(campusId);
        model.addAttribute("outboundRequests", outbound);
        
        // Inbound: Lô hàng đang trên đường giao đến cơ sở mình (InTransit)
        List<com.swp5.library_management.entity.TransferRequest> inbound = transferService.getWorkingInboundRequests(campusId);
        // Gom lô hàng (Batching) theo mốc thời gian xuất kho
        Map<java.time.LocalDateTime, List<com.swp5.library_management.entity.TransferRequest>> inboundGroups = inbound.stream()
                .filter(t -> t.getShippedAt() != null)
                .collect(Collectors.groupingBy(com.swp5.library_management.entity.TransferRequest::getShippedAt));
        model.addAttribute("inboundGroups", inboundGroups);
        
        // My Sent Requests: Yêu cầu xin sách từ cơ sở khác mà mình gửi đi (Đang chờ cơ sở kia duyệt)
        List<com.swp5.library_management.entity.TransferRequest> mySentRequests = transferService.getMyPendingRequestsToOtherCampuses(campusId);
        model.addAttribute("mySentRequests", mySentRequests);
        
        // Waitlist logic (giữ nguyên để support tạo lệnh mới)
        List<WaitlistHotspotDTO> hotspots = waitlistRepository.findSuggestedTransfers(campusId)
                .stream().limit(5).collect(Collectors.toList());
        model.addAttribute("waitlistHotspots", hotspots);

        return "inventory/transfers";
    }

    // 2. Hiển thị Giao diện Lịch sử (Transfer History)
    @GetMapping("/history")
    public String viewHistory(@org.springframework.web.bind.annotation.ModelAttribute("filter") com.swp5.library_management.dto.TransferFilterDTO filter, Model model, HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null || !user.isLibrarian())
            return "redirect:/login";

        filter.initDefaultDatesIfNull();
        model.addAttribute("data", transferService.getTransferHistory(filter));
        model.addAttribute("campuses", campusRepository.findAll());
        
        return "inventory/transfer-history";
    }

    // 3. API Kéo - Thả (Ajax) cập nhật trạng thái đơn (Accept/Reject)
    @PostMapping("/{id:\\d+}/status")
    @org.springframework.web.bind.annotation.ResponseBody
    public org.springframework.http.ResponseEntity<?> updateStatusAjax(
            @PathVariable("id") Integer transferId,
            @RequestParam("status") String status,
            @RequestParam(value = "note", required = false) String note,
            HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null || !user.isLibrarian()) 
            return org.springframework.http.ResponseEntity.status(401).body("Unauthorized");

        try {
            transferService.updateRequestStatus(transferId, status, note, user.getCampusId());
            return org.springframework.http.ResponseEntity.ok(Map.of("success", true, "message", "Đã cập nhật trạng thái"));
        } catch (Exception e) {
            return org.springframework.http.ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // 4. Batch Ship (Xác nhận xuất kho tất cả các đơn Accepted)
    @PostMapping("/batch-ship")
    public String batchShip(HttpSession session, RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user != null && user.isLibrarian()) {
            try {
                transferService.confirmBatchShipment(user.getCampusId());
                redirectAttributes.addFlashAttribute("successMsg", "Đã xuất lô hàng cho đơn vị vận chuyển!");
            } catch (Exception e) {
                redirectAttributes.addFlashAttribute("errorMsg", "Lỗi: " + e.getMessage());
            }
        }
        return "redirect:/librarian/inventory/transfers";
    }

    // 5. Batch Receive (Xác nhận nhập kho nguyên Lô)
    @PostMapping("/batch-receive")
    public String batchReceive(
            @RequestParam("shippedAt") @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime shippedAt,
            HttpSession session, RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user != null && user.isLibrarian()) {
            try {
                transferService.confirmBatchReceipt(user.getCampusId(), shippedAt, user.getUserId());
                redirectAttributes.addFlashAttribute("successMsg", "Xác nhận nhập kho lô hàng thành công!");
            } catch (Exception e) {
                redirectAttributes.addFlashAttribute("errorMsg", "Lỗi: " + e.getMessage());
            }
        }
        return "redirect:/librarian/inventory/transfers";
    }

    // 1. Hiển thị Form tạo lệnh
    @GetMapping("/create")
    public String showCreateForm(@RequestParam(value = "suggestedBookId", required = false) Integer suggestedBookId, Model model, HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null || !user.isLibrarian())
            return "redirect:/login";

        model.addAttribute("campuses", campusRepository.findAll());
        
        if (suggestedBookId != null) {
            // Lấy tất cả available copies của sách này trên TOÀN HỆ THỐNG
            List<BookCopy> availableCopies = bookCopyRepository.findByBookBookIdAndCopyStatus(suggestedBookId, "Available");
            
            // Map<CampusId, List<CopyId>>
            Map<Integer, List<String>> campusCopiesMap = availableCopies.stream()
                .collect(Collectors.groupingBy(
                    c -> c.getCampus().getCampusId(),
                    Collectors.mapping(BookCopy::getCopyId, Collectors.toList())
                ));
            
            model.addAttribute("campusCopiesMap", campusCopiesMap);
            model.addAttribute("suggestedBookId", suggestedBookId);
        }
        
        return "inventory/create-transfer";
    }

    // 2. Xử lý Form Submit để tạo lệnh
    @PostMapping("/create")
    public String processCreateTransfer(
            @RequestParam("fromCampusId") Integer fromCampusId,
            @RequestParam("toCampusId") Integer toCampusId,
            @RequestParam("copyIds") String copyIdsStr,
            @RequestParam(value = "note", required = false) String note,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null || !user.isLibrarian())
            return "redirect:/login";

        try {
            List<String> copyIds = Arrays.stream(copyIdsStr.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());

            transferService.createTransfer(fromCampusId, toCampusId, copyIds, user.getUserId(), note);
            redirectAttributes.addFlashAttribute("successMsg", "Tạo lệnh xin sách thành công!");
            return "redirect:/librarian/inventory/transfers";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", "Lỗi: " + e.getMessage());
            redirectAttributes.addFlashAttribute("suggestedCopyIds", copyIdsStr);
            redirectAttributes.addFlashAttribute("note", note);
            return "redirect:/librarian/inventory/transfers/create";
        }
    }

    // 3. Xem chi tiết một lệnh luân chuyển
    @GetMapping("/{id:\\d+}")
    public String viewTransferDetail(@PathVariable("id") Integer transferId, Model model, HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null || !user.isLibrarian())
            return "redirect:/login";

        model.addAttribute("transfer", transferService.getTransferById(transferId));
        return "inventory/transfer-detail";
    }

    // 4. Xác nhận HỦY LỆNH (Trường hợp tạo nhầm)
    @PostMapping("/{id:\\d+}/cancel")
    public String cancelTransfer(@PathVariable("id") Integer transferId, HttpSession session,
            RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user != null && user.isLibrarian()) {
            try {
                transferService.cancelTransfer(transferId, user.getCampusId());
                redirectAttributes.addFlashAttribute("successMsg", "Đã hủy lệnh luân chuyển thành công!");
            } catch (Exception e) {
                redirectAttributes.addFlashAttribute("errorMsg", "Lỗi hủy lệnh: " + e.getMessage());
            }
            return "redirect:/librarian/inventory/transfers";
        }
        return "redirect:/login";
    }

    // 5. Cập nhật trạng thái GIAO HÀNG (In Transit)
    @PostMapping("/{id:\\d+}/ship")
    public String shipTransfer(@PathVariable("id") Integer transferId, HttpSession session,
            RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user != null && user.isLibrarian()) {
            try {
                transferService.markAsInTransit(transferId, user.getCampusId());
                redirectAttributes.addFlashAttribute("successMsg", "Lô sách đã được giao cho đơn vị vận chuyển!");
            } catch (Exception e) {
                redirectAttributes.addFlashAttribute("errorMsg", "Lỗi xử lý: " + e.getMessage());
            }
            return "redirect:/librarian/inventory/transfers";
        }
        return "redirect:/login";
    }

    // 6. Xử lý hành động "Xác nhận nhập kho"
    @PostMapping("/{id:\\d+}/confirm")
    public String confirmTransferReceipt(@PathVariable("id") Integer transferId, HttpSession session,
            RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user != null && user.isLibrarian()) {
            try {
                transferService.confirmReceipt(transferId, user.getUserId());
                redirectAttributes.addFlashAttribute("successMsg", "Xác nhận nhập kho thành công!");
            } catch (Exception e) {
                redirectAttributes.addFlashAttribute("errorMsg", "Lỗi xác nhận: " + e.getMessage());
            }
            return "redirect:/librarian/inventory/transfers";
        }
        return "redirect:/login";
    }
}