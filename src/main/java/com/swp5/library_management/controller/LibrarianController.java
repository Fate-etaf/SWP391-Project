package com.swp5.library_management.controller;

import com.swp5.library_management.entity.*;
import com.swp5.library_management.repository.*;
import com.swp5.library_management.service.EmailService;
import com.swp5.library_management.service.SystemConfigService;
import com.swp5.library_management.service.MaterialRequestService;
import com.swp5.library_management.service.MaterialRequestExportService;
import com.swp5.library_management.service.UserStatusService;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/librarian")
@RequiredArgsConstructor
public class LibrarianController {

    private final UserRepository userRepository;
    private final BookCopyRepository bookCopyRepository;
    private final CampusRepository campusRepository;
    private final BorrowTicketRepository borrowTicketRepository;
    private final BorrowTicketDetailRepository borrowTicketDetailRepository;
    private final MaterialRequestRepository materialRequestRepository;
    private final SystemConfigService systemConfigService;
    private final ReservationRepository reservationRepository;
    private final EmailService emailService;
    private final MaterialRequestService materialRequestService;
    private final UserStatusService userStatusService;
    private final MaterialRequestExportService materialRequestExportService;

    private boolean isNotLibrarian(HttpSession session) {
        Boolean isLibrarian = (Boolean) session.getAttribute("isLibrarian");
        return isLibrarian == null || !isLibrarian;
    }

    // ── 2. CREATE LOAN RECORD (GET FORM) ──
    @GetMapping("/create-loan")
    public String showCreateLoanForm(HttpSession session, Model model, RedirectAttributes redirectAttrs) {
        if (isNotLibrarian(session)) {
            redirectAttrs.addFlashAttribute("errorMsg", "Quyền truy cập bị từ chối. Vui lòng đăng nhập bằng tài khoản Thủ thư.");
            return "redirect:/login";
        }
        return "librarian/create-loan";
    }

    // ── API: GET HOLDING RESERVATIONS FOR PATRON ──
    @GetMapping("/api/holding-reservations")
    @ResponseBody
    public org.springframework.http.ResponseEntity<?> getHoldingReservations(@RequestParam String patronId, HttpSession session) {
        if (isNotLibrarian(session)) {
            return org.springframework.http.ResponseEntity.status(403).body("Access Denied");
        }
        String loggedInUserId = (String) session.getAttribute("loggedInUserId");
        User librarian = null;
        if (loggedInUserId != null) {
            librarian = userRepository.findById(loggedInUserId).orElse(null);
        }
        if (librarian == null || librarian.getCampusId() == null) {
            return org.springframework.http.ResponseEntity.badRequest().body("Librarian campus unknown");
        }

        List<Reservation> reservations = reservationRepository.findByPatronUserIdAndStatusOrderByReservedAtDesc(patronId.trim(), "Holding");
        
        Integer libCampusId = librarian.getCampusId();
        var dtos = reservations.stream()
                .filter(r -> r.getCopy() != null && r.getCopy().getCampus() != null && r.getCopy().getCampus().getCampusId().equals(libCampusId))
                .map(r -> java.util.Map.of(
                        "reservationId", r.getReservationId(),
                        "copyId", r.getCopy().getCopyId(),
                        "bookTitle", r.getCopy().getBook().getTitle()
                )).toList();

        return org.springframework.http.ResponseEntity.ok(dtos);
    }

    // ── 3. CREATE LOAN RECORD (POST HANDLER) ──
    @PostMapping("/create-loan")
    @Transactional
    public String handleCreateLoan(@RequestParam String patronId,
                                   @RequestParam(required = false) List<String> copyIds,
                                   HttpSession session,
                                   RedirectAttributes redirectAttrs,
                                   Model model) {
        if (isNotLibrarian(session)) {
            redirectAttrs.addFlashAttribute("errorMsg", "Quyền truy cập bị từ chối. Vui lòng đăng nhập bằng tài khoản Thủ thư.");
            return "redirect:/login";
        }

        patronId = patronId.trim();
        List<String> validCopyIds = copyIds == null ? java.util.Collections.emptyList() : copyIds.stream().map(String::trim).filter(s -> !s.isEmpty()).distinct().toList();

        if (validCopyIds.isEmpty()) {
            model.addAttribute("errorMsg", "Vui lòng nhập ít nhất một Mã bản sao sách (Copy ID)!");
            model.addAttribute("patronId", patronId);
            return "librarian/create-loan";
        }

        // Validate patron
        Optional<User> patronOpt = userRepository.findById(patronId);
        if (patronOpt.isEmpty()) {
            model.addAttribute("errorMsg", "Mã số bạn đọc (Patron ID) không tồn tại trên hệ thống!");
            model.addAttribute("patronId", patronId);
            return "librarian/create-loan";
        }
        User patron = patronOpt.get();
        if (!"Active".equalsIgnoreCase(patron.getStatus())) {
            model.addAttribute("errorMsg", "Tài khoản bạn đọc đang bị khóa hoặc không hoạt động!");
            model.addAttribute("patronId", patronId);
            return "librarian/create-loan";
        }
        if (patron.getBorrowingLocked() != null && patron.getBorrowingLocked()) {
            model.addAttribute("errorMsg", "Tài khoản bạn đọc hiện đang bị khóa chức năng mượn sách!");
            model.addAttribute("patronId", patronId);
            return "librarian/create-loan";
        }
        
        String granularStatus = userStatusService.calculateSingleStatus(patronId, patron.getStatus());
        if ("Under Penalty".equals(granularStatus)) {
            model.addAttribute("errorMsg", "Bạn đọc đang có phiếu phạt chưa nộp, không thể mượn thêm sách!");
            model.addAttribute("patronId", patronId);
            return "librarian/create-loan";
        }
        if ("Overdue".equals(granularStatus)) {
            model.addAttribute("errorMsg", "Bạn đọc đang có sách mượn quá hạn, không thể mượn thêm sách!");
            model.addAttribute("patronId", patronId);
            return "librarian/create-loan";
        }
        if ("Graduated".equals(granularStatus) || "Inactive".equals(granularStatus)) {
            model.addAttribute("errorMsg", "Tài khoản không hoạt động hoặc đã tốt nghiệp, không thể mượn sách!");
            model.addAttribute("patronId", patronId);
            return "librarian/create-loan";
        }

        // Check borrowing limit
        int currentBorrowedCount = borrowTicketDetailRepository.countActiveBorrowedByPatronId(patronId);
        int maxAllowed = systemConfigService.getIntConfig("MAX_BOOKS_STUDENT", 3);
        List<String> patronRoles = userRepository.findRolesByUserId(patronId);
        if (patronRoles.contains("Lecturer")) {
            maxAllowed = systemConfigService.getIntConfig("MAX_BOOKS_LECTURER", 10);
        }

        if (currentBorrowedCount + validCopyIds.size() > maxAllowed) {
            model.addAttribute("errorMsg", "Vượt quá giới hạn! Bạn đọc đang mượn " + currentBorrowedCount + " cuốn, mượn thêm " + validCopyIds.size() + " cuốn sẽ vượt mức tối đa " + maxAllowed + " cuốn.");
            model.addAttribute("patronId", patronId);
            return "librarian/create-loan";
        }

        // Validate book copies before saving anything
        List<BookCopy> copiesToBorrow = new java.util.ArrayList<>();
        List<Reservation> matchedReservations = new java.util.ArrayList<>();
        java.util.Set<Integer> bookIdsInThisTransaction = new java.util.HashSet<>();

        for (String copyId : validCopyIds) {
            Optional<BookCopy> copyOpt = bookCopyRepository.findById(copyId);
            if (copyOpt.isEmpty()) {
                model.addAttribute("errorMsg", "Mã bản sao sách (Copy ID) '" + copyId + "' không tồn tại trên hệ thống!");
                model.addAttribute("patronId", patronId);
                return "librarian/create-loan";
            }
            BookCopy copy = copyOpt.get();

            // Duplicate Book Check 1: Already borrowing
            boolean alreadyBorrowing = borrowTicketDetailRepository.existsActiveBorrowingByPatronAndBook(patronId, copy.getBook().getBookId());
            if (alreadyBorrowing) {
                model.addAttribute("errorMsg", "Bạn đọc đang mượn một bản sao của cuốn '" + copy.getBook().getTitle() + "'. Không thể mượn thêm bản sao khác cùng đầu sách!");
                model.addAttribute("patronId", patronId);
                return "librarian/create-loan";
            }

            // Duplicate Book Check 2: Same book multiple times in this transaction
            if (!bookIdsInThisTransaction.add(copy.getBook().getBookId())) {
                model.addAttribute("errorMsg", "Phát hiện 2 bản sao của cùng một đầu sách '" + copy.getBook().getTitle() + "' trong danh sách mượn. Không thể tạo đơn!");
                model.addAttribute("patronId", patronId);
                return "librarian/create-loan";
            }

            Reservation matchedReservation = null;
            if ("Reserved".equalsIgnoreCase(copy.getCopyStatus())) {
                List<Reservation> reservations = reservationRepository.findByPatronUserIdAndStatusOrderByReservedAtDesc(patronId, "Holding");
                for (Reservation r : reservations) {
                    if (r.getCopy() != null && r.getCopy().getCopyId().equals(copyId)) {
                        matchedReservation = r;
                        break;
                    }
                }
                if (matchedReservation == null) {
                    model.addAttribute("errorMsg", "Bản sao sách '" + copyId + "' hiện đang được đặt giữ chỗ bởi một Bạn đọc khác!");
                    model.addAttribute("patronId", patronId);
                    return "librarian/create-loan";
                }
            } else if (!"Available".equalsIgnoreCase(copy.getCopyStatus())) {
                model.addAttribute("errorMsg", "Bản sao sách '" + copyId + "' hiện không sẵn sàng để mượn (Trạng thái hiện tại: " + copy.getCopyStatus() + ")!");
                model.addAttribute("patronId", patronId);
                return "librarian/create-loan";
            }

            copiesToBorrow.add(copy);
            if (matchedReservation != null) {
                matchedReservations.add(matchedReservation);
            }
        }

        // Determine loan duration
        int loanDays = systemConfigService.getIntConfig("LOAN_DAYS_STUDENT", 14);
        if (patronRoles.contains("Lecturer")) {
            loanDays = systemConfigService.getIntConfig("LOAN_DAYS_LECTURER", 30);
        }

        String loggedInUserId = (String) session.getAttribute("loggedInUserId");
        User librarian = null;
        Campus librarianCampus = null;
        if (loggedInUserId != null) {
            librarian = userRepository.findById(loggedInUserId).orElse(null);
            if (librarian != null && librarian.getCampusId() != null) {
                librarianCampus = campusRepository.findById(librarian.getCampusId()).orElse(null);
            }
        }

        // Create exactly 1 BorrowTicket
        BorrowTicket ticket = new BorrowTicket();
        ticket.setPatron(patron);
        ticket.setLibrarian(librarian);
        ticket.setCampus(librarianCampus);
        ticket.setCreatedAt(LocalDateTime.now());
        ticket.setNote("Được tạo bởi thủ thư tại quầy (" + validCopyIds.size() + " cuốn)");
        ticket = borrowTicketRepository.save(ticket);

        // Process each copy
        for (BookCopy copy : copiesToBorrow) {
            BorrowTicketDetail detail = new BorrowTicketDetail();
            detail.setBorrowTicket(ticket);
            detail.setBookCopy(copy);
            detail.setDueDate(LocalDateTime.now().plusDays(loanDays));
            detail.setRenewalCount(0);
            detail.setStatus("Borrowing");
            detail.setReturnDate(null);
            borrowTicketDetailRepository.save(detail);

            copy.setCopyStatus("Borrowed");
            bookCopyRepository.save(copy);

            String formattedDueDate = detail.getDueDate().format(DateTimeFormatter.ofPattern("HH:mm 'ngày' dd/MM/yyyy"));
            emailService.sendLoanConfirmation(
                    patron.getEmail(),
                    patron.getFullName(),
                    copy.getBook().getTitle(),
                    copy.getCopyId(),
                    formattedDueDate
            );
        }

        // Update Reservation status if checkout for a reserved copy
        for (Reservation res : matchedReservations) {
            res.setStatus("Completed");
            reservationRepository.save(res);
        }

        redirectAttrs.addFlashAttribute("successMsg", "Tạo đơn mượn sách thành công! Đã xử lý " + validCopyIds.size() + " bản sao cho bạn đọc " + patron.getFullName() + ".");
        return "redirect:/librarian/create-loan";
    }
    // ── 4. ACQUISITION DASHBOARD ──
    @GetMapping("/acquisition/dashboard")
    public String dashboard(@RequestParam(required = false) String status,
                            @RequestParam(required = false) String patronRole,
                            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate fromDate,
                            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate toDate,
                            HttpSession session,
                            Model model,
                            RedirectAttributes redirectAttrs) {
        if (isNotLibrarian(session)) {
            redirectAttrs.addFlashAttribute("errorMsg", "Quyền truy cập bị từ chối.");
            return "redirect:/login";
        }

        String loggedInUserId = (String) session.getAttribute("loggedInUserId");
        User librarian = (loggedInUserId != null)
                ? userRepository.findById(loggedInUserId).orElse(null)
                : null;
        Integer librarianCampusId = (librarian != null) ? librarian.getCampusId() : null;
        
        // Stats scoped to this librarian's campus
        long pendingCount  = (librarianCampusId != null)
                ? materialRequestRepository.countByStatusAndPatronCampusId("Pending",  librarianCampusId)
                : materialRequestRepository.countByStatus("Pending");
        long approvedCount = (librarianCampusId != null)
                ? materialRequestRepository.countByStatusAndPatronCampusId("Approved", librarianCampusId)
                : materialRequestRepository.countByStatus("Approved");
        long rejectedCount = (librarianCampusId != null)
                ? materialRequestRepository.countByStatusAndPatronCampusId("Rejected", librarianCampusId)
                : materialRequestRepository.countByStatus("Rejected");
        long availableCount = (librarianCampusId != null)
                ? materialRequestRepository.countByStatusAndPatronCampusId("Available", librarianCampusId)
                : materialRequestRepository.countByStatus("Available");

        LocalDateTime fromDateTime = (fromDate != null) ? fromDate.atStartOfDay() : null;
        LocalDateTime toDateTime = (toDate != null) ? toDate.atTime(23, 59, 59) : null;

        // Request list scoped to this librarian's campus
        List<MaterialRequest> requests = (librarianCampusId != null)
                ? materialRequestRepository.findByStatusAndSearchTermAndCampusId(status, null, librarianCampusId, fromDateTime, toDateTime)
                : materialRequestRepository.findByStatusAndSearchTerm(status, null, fromDateTime, toDateTime);

        // Filter by Patron Role (Student / Lecturer)
        if (patronRole != null && !patronRole.isEmpty()) {
            if ("Student".equalsIgnoreCase(patronRole)) {
                requests = requests.stream()
                        .filter(r -> r.getPatron() != null &&
                                (r.getPatron().getRoles().isEmpty()
                                 || r.getPatron().getRoles().stream().anyMatch(role -> Integer.valueOf(1).equals(role.getRoleId()))))
                        .toList();
            } else if ("Lecturer".equalsIgnoreCase(patronRole)) {
                requests = requests.stream()
                        .filter(r -> r.getPatron() != null &&
                                r.getPatron().getRoles().stream().anyMatch(role -> Integer.valueOf(2).equals(role.getRoleId())))
                        .toList();
            }
        }
        

        model.addAttribute("pendingCount",       pendingCount);
        model.addAttribute("approvedCount",      approvedCount);
        model.addAttribute("rejectedCount",      rejectedCount);
        model.addAttribute("availableCount",     availableCount);
        model.addAttribute("requests",           requests);
        model.addAttribute("currentStatus",      status);
        model.addAttribute("currentPatronRole",  patronRole);
        model.addAttribute("currentFromDate",    fromDate);
        model.addAttribute("currentToDate",      toDate);
        model.addAttribute("librarianCampusId",  librarianCampusId);

        return "acquisition/dashboard";
    }

    @GetMapping("/acquisition/export-excel")
    public void exportToExcel(@RequestParam(required = false) String status,
                              @RequestParam(required = false) String patronRole,
                              @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate fromDate,
                              @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate toDate,
                              HttpSession session,
                              jakarta.servlet.http.HttpServletResponse response) throws java.io.IOException {
        if (isNotLibrarian(session)) {
            response.sendRedirect("/login");
            return;
        }

        String loggedInUserId = (String) session.getAttribute("loggedInUserId");
        User librarian = (loggedInUserId != null)
                ? userRepository.findById(loggedInUserId).orElse(null)
                : null;
        Integer librarianCampusId = (librarian != null) ? librarian.getCampusId() : null;

        LocalDateTime fromDateTime = (fromDate != null) ? fromDate.atStartOfDay() : null;
        LocalDateTime toDateTime = (toDate != null) ? toDate.atTime(23, 59, 59) : null;

        List<MaterialRequest> requests = (librarianCampusId != null)
                ? materialRequestRepository.findByStatusAndSearchTermAndCampusId(status, null, librarianCampusId, fromDateTime, toDateTime)
                : materialRequestRepository.findByStatusAndSearchTerm(status, null, fromDateTime, toDateTime);

        // Filter by Patron Role
        if (patronRole != null && !patronRole.isEmpty()) {
            if ("Student".equalsIgnoreCase(patronRole)) {
                requests = requests.stream()
                        .filter(r -> r.getPatron() != null &&
                                (r.getPatron().getRoles().isEmpty()
                                 || r.getPatron().getRoles().stream().anyMatch(role -> Integer.valueOf(1).equals(role.getRoleId()))))
                        .toList();
            } else if ("Lecturer".equalsIgnoreCase(patronRole)) {
                requests = requests.stream()
                        .filter(r -> r.getPatron() != null &&
                                r.getPatron().getRoles().stream().anyMatch(role -> Integer.valueOf(2).equals(role.getRoleId())))
                        .toList();
            }
        }

        byte[] excelData = materialRequestExportService.exportToExcel(requests);

        String fileName = "material_requests_" + java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")) + ".xlsx";
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
        response.getOutputStream().write(excelData);
        response.getOutputStream().flush();
    }

    @PostMapping("/acquisition/approve/{id}")
    public String approveRequest(@PathVariable("id") Integer requestId, HttpSession session, RedirectAttributes redirectAttrs) {
        if (isNotLibrarian(session)) {
            redirectAttrs.addFlashAttribute("errorMsg", "Quyền truy cập bị từ chối.");
            return "redirect:/login";
        }
        
        String loggedInUserId = (String) session.getAttribute("loggedInUserId");
        try {
            materialRequestService.approveRequest(requestId, loggedInUserId);
            redirectAttrs.addFlashAttribute("successMsg", "Yêu cầu đề nghị tài liệu (REQ" + String.format("%03d", requestId) + ") đã được duyệt thành công!");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("errorMsg", "Có lỗi xảy ra: " + e.getMessage());
        }
        
        return "redirect:/librarian/acquisition/dashboard";
    }

    @PostMapping("/acquisition/reject/{id}")
    public String rejectRequest(@PathVariable("id") Integer requestId, HttpSession session, RedirectAttributes redirectAttrs) {
        if (isNotLibrarian(session)) {
            redirectAttrs.addFlashAttribute("errorMsg", "Quyền truy cập bị từ chối.");
            return "redirect:/login";
        }
        
        String loggedInUserId = (String) session.getAttribute("loggedInUserId");
        try {
            materialRequestService.rejectRequest(requestId, loggedInUserId);
            redirectAttrs.addFlashAttribute("successMsg", "Yêu cầu đề nghị tài liệu (REQ" + String.format("%03d", requestId) + ") đã bị từ chối thành công!");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("errorMsg", "Có lỗi xảy ra: " + e.getMessage());
        }
        
        return "redirect:/librarian/acquisition/dashboard";
    }

    @PostMapping("/acquisition/batch-action")
    public String batchAction(@RequestParam(value = "requestIds", required = false) List<Integer> requestIds,
                               @RequestParam(value = "action", required = false) String action,
                               HttpSession session,
                               RedirectAttributes redirectAttrs) {
        if (isNotLibrarian(session)) {
            redirectAttrs.addFlashAttribute("errorMsg", "Quyền truy cập bị từ chối.");
            return "redirect:/login";
        }

        if (requestIds == null || requestIds.isEmpty()) {
            redirectAttrs.addFlashAttribute("errorMsg", "Vui lòng chọn ít nhất một yêu cầu.");
            return "redirect:/librarian/acquisition/dashboard";
        }

        String loggedInUserId = (String) session.getAttribute("loggedInUserId");
        int successCount = 0;
        int failCount = 0;
        StringBuilder errorMsgBuilder = new StringBuilder();

        for (Integer id : requestIds) {
            try {
                if ("approve".equalsIgnoreCase(action)) {
                    materialRequestService.approveRequest(id, loggedInUserId);
                    successCount++;
                } else if ("reject".equalsIgnoreCase(action)) {
                    materialRequestService.rejectRequest(id, loggedInUserId);
                    successCount++;
                }
            } catch (Exception e) {
                failCount++;
                errorMsgBuilder.append("REQ").append(String.format("%03d", id)).append(": ").append(e.getMessage()).append("; ");
            }
        }

        if (failCount == 0) {
            redirectAttrs.addFlashAttribute("successMsg", "Đã xử lý thành công " + successCount + " yêu cầu.");
        } else {
            redirectAttrs.addFlashAttribute("successMsg", "Đã xử lý thành công " + successCount + " yêu cầu.");
            redirectAttrs.addFlashAttribute("errorMsg", "Thất bại " + failCount + " yêu cầu. Chi tiết: " + errorMsgBuilder.toString());
        }

        return "redirect:/librarian/acquisition/dashboard";
    }

    @GetMapping("/acquisition/view/{id}")
    public String viewRequestDetail(@PathVariable("id") Integer requestId, HttpSession session, Model model, RedirectAttributes redirectAttrs) {
        if (isNotLibrarian(session)) {
            redirectAttrs.addFlashAttribute("errorMsg", "Quyền truy cập bị từ chối.");
            return "redirect:/login";
        }
        
        MaterialRequest request = materialRequestRepository.findById(requestId).orElse(null);
        if (request == null) {
            redirectAttrs.addFlashAttribute("errorMsg", "Không tìm thấy yêu cầu.");
            return "redirect:/librarian/acquisition/dashboard";
        }

        String loggedInUserId = (String) session.getAttribute("loggedInUserId");
        User librarian = (loggedInUserId != null)
                ? userRepository.findById(loggedInUserId).orElse(null)
                : null;
        Integer librarianCampusId = (librarian != null) ? librarian.getCampusId() : null;
        
        model.addAttribute("req", request);
        model.addAttribute("librarianCampusId", librarianCampusId);
        return "services/request-material";
    }
}
