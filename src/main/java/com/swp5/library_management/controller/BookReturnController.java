package com.swp5.library_management.controller;

import com.swp5.library_management.entity.BorrowTicketDetail;
import com.swp5.library_management.service.BookReturnService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

@Controller
@RequestMapping("/borrowing")
@RequiredArgsConstructor
public class BookReturnController {

    private final BookReturnService bookReturnService;
    private final com.swp5.library_management.repository.BorrowTicketDetailRepository borrowTicketDetailRepository;
    private final com.swp5.library_management.repository.FineInvoiceRepository fineInvoiceRepository;
    private static final ConcurrentLinkedQueue<String> scanQueue = new ConcurrentLinkedQueue<>();

    @org.springframework.beans.factory.annotation.Value("${app.vietqr.bank-id}")
    private String bankId;

    @org.springframework.beans.factory.annotation.Value("${app.vietqr.bank-name}")
    private String bankName;

    @org.springframework.beans.factory.annotation.Value("${app.vietqr.account-no}")
    private String accountNo;

    @org.springframework.beans.factory.annotation.Value("${app.vietqr.account-name}")
    private String accountName;


    private boolean isNotLibrarian(HttpSession session) {
        Boolean isLibrarian = (Boolean) session.getAttribute("isLibrarian");
        return isLibrarian == null || !isLibrarian;
    }

    @GetMapping
    public String showBorrowingList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) String bookTitle,
            @RequestParam(required = false) String borrowerId,
            HttpSession session,
            RedirectAttributes redirectAttrs,
            Model model) {
        if (isNotLibrarian(session)) {
            redirectAttrs.addFlashAttribute("errorMsg", "Quyền truy cập bị từ chối. Vui lòng đăng nhập bằng tài khoản Thủ thư.");
            return "redirect:/login";
        }
        
        String librarianId = (String) session.getAttribute("loggedInUserId");
        List<BorrowTicketDetail> list = bookReturnService.searchCurrentlyBorrowing(bookTitle, borrowerId, librarianId);

        int pageSize = 10;
        int totalPages = (int) Math.ceil((double) list.size() / pageSize);
        int safePage = Math.max(0, Math.min(page, Math.max(totalPages - 1, 0)));

        int from = safePage * pageSize;
        int to = Math.min(from + pageSize, list.size());
        List<BorrowTicketDetail> pagedList = list.isEmpty() ? list : list.subList(from, to);

        model.addAttribute("borrowingItems", pagedList);
        model.addAttribute("currentPage", safePage);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("bookTitle", bookTitle);
        model.addAttribute("borrowerId", borrowerId);

        model.addAttribute("vietqrBankId", bankId);
        model.addAttribute("vietqrBankName", bankName);
        model.addAttribute("vietqrAccountNo", accountNo);
        model.addAttribute("vietqrAccountName", accountName);

        return "violations/borrowing-list";
    }

    @GetMapping("/return-center")
    public String showReturnCenter(HttpSession session, RedirectAttributes redirectAttrs) {
        if (isNotLibrarian(session)) {
            redirectAttrs.addFlashAttribute("errorMsg", "Quyền truy cập bị từ chối. Vui lòng đăng nhập bằng tài khoản Thủ thư.");
            return "redirect:/login";
        }
        return "redirect:/borrowing";
    }

    @GetMapping("/check-scan")
    @ResponseBody
    public ResponseEntity<?> checkScan(@RequestParam String copyId, HttpSession session) {
        if (isNotLibrarian(session)) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Quyền truy cập bị từ chối. Vui lòng đăng nhập bằng tài khoản Thủ thư."));
        }
        try {
            Map<String, Object> info = bookReturnService.checkScan(copyId);
            return ResponseEntity.ok(info);
        } catch (Exception e) {
            e.printStackTrace();
            String errorMsg = e.getMessage();
            if (errorMsg == null || errorMsg.isBlank()) {
                errorMsg = "Lỗi hệ thống khi kiểm tra thông tin sách (" + e.getClass().getSimpleName() + ")";
            }
            return ResponseEntity.badRequest().body(Map.of("message", errorMsg));
        }
    }

    @PostMapping("/api/push-scan")
    @ResponseBody
    @CrossOrigin(origins = "*")
    public ResponseEntity<?> pushScanFromDevice(
            @RequestParam(required = false) String copyId,
            jakarta.servlet.http.HttpServletRequest request) {

        String finalCopyId = copyId;
        if (finalCopyId == null || finalCopyId.isBlank()) {
            finalCopyId = request.getParameter("copyId");
        }

        if (finalCopyId == null || finalCopyId.isBlank()) {
            String contentType = request.getContentType();
            if (contentType != null && contentType.toLowerCase().contains("application/json")) {
                try {
                    String bodyStr = request.getReader().lines()
                            .collect(java.util.stream.Collectors.joining(System.lineSeparator()));
                    if (bodyStr != null && bodyStr.contains("copyId")) {
                        java.util.regex.Pattern p = java.util.regex.Pattern.compile("\"copyId\"\\s*:\\s*\"([^\"]+)\"");
                        java.util.regex.Matcher m = p.matcher(bodyStr);
                        if (m.find()) {
                            finalCopyId = m.group(1);
                        } else {
                            java.util.regex.Pattern p2 = java.util.regex.Pattern.compile("copyId\\s*:\\s*\"([^\"]+)\"");
                            java.util.regex.Matcher m2 = p2.matcher(bodyStr);
                            if (m2.find()) {
                                finalCopyId = m2.group(1);
                            }
                        }
                    }
                } catch (Exception e) {
                    // log raw body parsing error
                }
            }
        }

        if (finalCopyId == null || finalCopyId.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "copyId không được để trống"));
        }

        scanQueue.offer(finalCopyId.trim());
        return ResponseEntity.ok(Map.of("status", "RECEIVED", "copyId", finalCopyId.trim(),
                "message", "Đã nhận mã quét, web sẽ tự động xử lý."));
    }

    @GetMapping("/api/poll-scan")
    @ResponseBody
    public ResponseEntity<?> pollScan(HttpSession session) {
        if (isNotLibrarian(session)) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Quyền truy cập bị từ chối. Vui lòng đăng nhập bằng tài khoản Thủ thư."));
        }
        String copyId = scanQueue.poll();
        if (copyId == null) {
            return ResponseEntity.ok(Map.of("status", "PENDING"));
        }
        return ResponseEntity.ok(Map.of("status", "SCANNED", "copyId", copyId));
    }

    // -----------------------------------------------------------
    // Action Redirect Endpoints for borrowing-list.html
    // -----------------------------------------------------------
    @PostMapping("/return/{id}")
    public String confirmReturn(@PathVariable("id") Integer id, HttpSession session, RedirectAttributes redirectAttrs) {
        if (isNotLibrarian(session)) {
            redirectAttrs.addFlashAttribute("errorMsg", "Quyền truy cập bị từ chối. Vui lòng đăng nhập bằng tài khoản Thủ thư.");
            return "redirect:/login";
        }
        try {
            String librarianId = (String) session.getAttribute("loggedInUserId");
            // Trả sách qua form redirect: giữ nguyên tình trạng (null = không đổi)
            bookReturnService.processNormalReturn(id, null, librarianId);
            redirectAttrs.addFlashAttribute("successMsg", "Trả sách thành công!");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("errorMsg", "Lỗi: " + e.getMessage());
        }
        return "redirect:/borrowing";
    }

    @PostMapping("/create-lost/{id}")
    public String confirmLost(@PathVariable("id") Integer id, HttpSession session, RedirectAttributes redirectAttrs) {
        if (isNotLibrarian(session)) {
            redirectAttrs.addFlashAttribute("errorMsg", "Quyền truy cập bị từ chối. Vui lòng đăng nhập bằng tài khoản Thủ thư.");
            return "redirect:/login";
        }
        try {
            String librarianId = (String) session.getAttribute("loggedInUserId");
            bookReturnService.processLost(id, "Cash", null, librarianId, null);
            redirectAttrs.addFlashAttribute("successMsg", "Đã đánh dấu sách bị mất!");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("errorMsg", "Lỗi: " + e.getMessage());
        }
        return "redirect:/borrowing";
    }

    @PostMapping("/create-damaged/{id}")
    public String confirmDamaged(@PathVariable("id") Integer id, HttpSession session, RedirectAttributes redirectAttrs) {
        if (isNotLibrarian(session)) {
            redirectAttrs.addFlashAttribute("errorMsg", "Quyền truy cập bị từ chối. Vui lòng đăng nhập bằng tài khoản Thủ thư.");
            return "redirect:/login";
        }
        try {
            String librarianId = (String) session.getAttribute("loggedInUserId");
            bookReturnService.processDamaged(id, "Cash", null, librarianId, null);
            redirectAttrs.addFlashAttribute("successMsg", "Đã đánh dấu sách bị hỏng!");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("errorMsg", "Lỗi: " + e.getMessage());
        }
        return "redirect:/borrowing";
    }

    // -----------------------------------------------------------
    // AJAX Submit Endpoints for return center
    // -----------------------------------------------------------
    @PostMapping("/api/return")
    @ResponseBody
    public ResponseEntity<?> apiConfirmReturn(
            @RequestParam Integer ticketDetailId,
            @RequestParam(required = false) String conditionStatus,
            HttpSession session) {
        if (isNotLibrarian(session)) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Quyền truy cập bị từ chối. Vui lòng đăng nhập bằng tài khoản Thủ thư."));
        }
        try {
            String librarianId = (String) session.getAttribute("loggedInUserId");
            bookReturnService.processNormalReturn(ticketDetailId, conditionStatus, librarianId);
            return ResponseEntity.ok(Map.of("status", "SUCCESS", "message", "Trả sách đúng hạn thành công!"));
        } catch (Exception e) {
            String errorMsg = e.getMessage();
            if (errorMsg == null || errorMsg.isBlank()) {
                errorMsg = "Lỗi hệ thống: " + e.getClass().getSimpleName();
            }
            return ResponseEntity.badRequest().body(Map.of("message", errorMsg));
        }
    }

    @PostMapping("/api/return-overdue")
    @ResponseBody
    public ResponseEntity<?> apiConfirmOverdueReturn(
            @RequestParam Integer ticketDetailId,
            @RequestParam(required = false) String paymentMethod,
            @RequestParam(required = false) String transactionCode,
            @RequestParam(required = false) String conditionStatus,
            HttpSession session) {
        if (isNotLibrarian(session)) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Quyền truy cập bị từ chối. Vui lòng đăng nhập bằng tài khoản Thủ thư."));
        }
        try {
            String librarianId = (String) session.getAttribute("loggedInUserId");
            bookReturnService.processOverdueReturn(ticketDetailId, paymentMethod, transactionCode, librarianId,
                    conditionStatus);
            return ResponseEntity
                    .ok(Map.of("status", "SUCCESS", "message", "Thu tiền và nhận trả sách quá hạn thành công!"));
        } catch (Exception e) {
            String errorMsg = e.getMessage();
            if (errorMsg == null || errorMsg.isBlank()) {
                errorMsg = "Lỗi hệ thống: " + e.getClass().getSimpleName();
            }
            return ResponseEntity.badRequest().body(Map.of("message", errorMsg));
        }
    }

    @PostMapping("/api/return-lost")
    @ResponseBody
    public ResponseEntity<?> apiConfirmLost(
            @RequestParam Integer ticketDetailId,
            @RequestParam(required = false) String paymentMethod,
            @RequestParam(required = false) String transactionCode,
            @RequestParam(required = false) String notes,
            HttpSession session) {
        if (isNotLibrarian(session)) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Quyền truy cập bị từ chối. Vui lòng đăng nhập bằng tài khoản Thủ thư."));
        }
        try {
            String librarianId = (String) session.getAttribute("loggedInUserId");
            bookReturnService.processLost(ticketDetailId, paymentMethod, transactionCode, librarianId, notes);
            return ResponseEntity
                    .ok(Map.of("status", "SUCCESS", "message", "Báo mất sách và lập hóa đơn phạt thành công!"));
        } catch (Exception e) {
            String errorMsg = e.getMessage();
            if (errorMsg == null || errorMsg.isBlank()) {
                errorMsg = "Lỗi hệ thống: " + e.getClass().getSimpleName();
            }
            return ResponseEntity.badRequest().body(Map.of("message", errorMsg));
        }
    }

    @PostMapping("/api/return-damaged")
    @ResponseBody
    public ResponseEntity<?> apiConfirmDamaged(
            @RequestParam Integer ticketDetailId,
            @RequestParam(required = false) String paymentMethod,
            @RequestParam(required = false) String transactionCode,
            @RequestParam(required = false) String notes,
            HttpSession session) {
        if (isNotLibrarian(session)) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Quyền truy cập bị từ chối. Vui lòng đăng nhập bằng tài khoản Thủ thư."));
        }
        try {
            String librarianId = (String) session.getAttribute("loggedInUserId");
            bookReturnService.processDamaged(ticketDetailId, paymentMethod, transactionCode, librarianId, notes);
            return ResponseEntity
                    .ok(Map.of("status", "SUCCESS", "message", "Báo hỏng sách và lập hóa đơn phạt thành công!"));
        } catch (Exception e) {
            String errorMsg = e.getMessage();
            if (errorMsg == null || errorMsg.isBlank()) {
                errorMsg = "Lỗi hệ thống: " + e.getClass().getSimpleName();
            }
            return ResponseEntity.badRequest().body(Map.of("message", errorMsg));
        }
    }

    @GetMapping("/api/check-return-status/{ticketDetailId}")
    @ResponseBody
    public ResponseEntity<?> checkReturnStatus(@PathVariable Integer ticketDetailId, HttpSession session) {
        if (isNotLibrarian(session)) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Quyền truy cập bị từ chối. Vui lòng đăng nhập bằng tài khoản Thủ thư."));
        }
        try {
            String loggedInUserId = (String) session.getAttribute("loggedInUserId");
            if (loggedInUserId != null) {
                bookReturnService.registerActiveLibrarian(ticketDetailId, loggedInUserId);
            }
            boolean isReturned = bookReturnService.isBookReturned(ticketDetailId);
            return ResponseEntity.ok(Map.of("success", true, "returned", isReturned));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/api/generate-payment-token")
    @ResponseBody
    public ResponseEntity<?> generatePaymentToken(
            @RequestParam String action,
            @RequestParam Integer id,
            HttpSession session) {
        if (isNotLibrarian(session)) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Quyền truy cập bị từ chối. Vui lòng đăng nhập bằng tài khoản Thủ thư."));
        }

        try {
            String patronId = null;
            if ("T".equalsIgnoreCase(action) || "M".equalsIgnoreCase(action) || "D".equalsIgnoreCase(action)) {
                com.swp5.library_management.entity.BorrowTicketDetail detail = borrowTicketDetailRepository.findById(id).orElse(null);
                if (detail != null && detail.getBorrowTicket() != null && detail.getBorrowTicket().getPatron() != null) {
                    patronId = detail.getBorrowTicket().getPatron().getUserId();
                }
            } else if ("F".equalsIgnoreCase(action)) {
                com.swp5.library_management.entity.FineInvoice fine = fineInvoiceRepository.findById(id).orElse(null);
                if (fine != null && fine.getPatron() != null) {
                    patronId = fine.getPatron().getUserId();
                }
            }

            String librarianId = (String) session.getAttribute("loggedInUserId");
            
            String token = com.swp5.library_management.utils.PaymentTokenUtil.generateToken(action, id, patronId, librarianId);
            return ResponseEntity.ok(Map.of("success", true, "token", token));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}

