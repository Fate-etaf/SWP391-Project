package com.swp5.library_management.controller;

import com.swp5.library_management.entity.*;
import com.swp5.library_management.repository.*;
import com.swp5.library_management.service.EmailService;
import com.swp5.library_management.service.SystemConfigService;
import com.swp5.library_management.service.MaterialRequestService;

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
                                   @RequestParam String copyId,
                                   HttpSession session,
                                   RedirectAttributes redirectAttrs,
                                   Model model) {
        if (isNotLibrarian(session)) {
            redirectAttrs.addFlashAttribute("errorMsg", "Quyền truy cập bị từ chối. Vui lòng đăng nhập bằng tài khoản Thủ thư.");
            return "redirect:/login";
        }

        patronId = patronId.trim();
        copyId = copyId.trim();

        // Validate patron
        Optional<User> patronOpt = userRepository.findById(patronId);
        if (patronOpt.isEmpty()) {
            model.addAttribute("errorMsg", "Mã số bạn đọc (Patron ID) không tồn tại trên hệ thống!");
            model.addAttribute("patronId", patronId);
            model.addAttribute("copyId", copyId);
            return "librarian/create-loan";
        }
        User patron = patronOpt.get();
        if (!"Active".equalsIgnoreCase(patron.getStatus())) {
            model.addAttribute("errorMsg", "Tài khoản bạn đọc đang bị khóa hoặc không hoạt động!");
            model.addAttribute("patronId", patronId);
            model.addAttribute("copyId", copyId);
            return "librarian/create-loan";
        }
        if (patron.getBorrowingLocked() != null && patron.getBorrowingLocked()) {
            model.addAttribute("errorMsg", "Tài khoản bạn đọc hiện đang bị khóa chức năng mượn sách!");
            model.addAttribute("patronId", patronId);
            model.addAttribute("copyId", copyId);
            return "librarian/create-loan";
        }

        // Check borrowing limit
        int currentBorrowedCount = borrowTicketDetailRepository.countActiveBorrowedByPatronId(patronId);
        int maxAllowed = systemConfigService.getIntConfig("MAX_BOOKS_STUDENT", 3);
        if (currentBorrowedCount >= maxAllowed) {
            model.addAttribute("errorMsg", "Bạn đọc đã vượt quá giới hạn mượn sách song hành (Đang mượn " + currentBorrowedCount + "/" + maxAllowed + " cuốn)!");
            model.addAttribute("patronId", patronId);
            model.addAttribute("copyId", copyId);
            return "librarian/create-loan";
        }

        // Validate book copy
        Optional<BookCopy> copyOpt = bookCopyRepository.findById(copyId);
        if (copyOpt.isEmpty()) {
            model.addAttribute("errorMsg", "Mã bản sao sách (Copy ID) không tồn tại trên hệ thống!");
            model.addAttribute("patronId", patronId);
            model.addAttribute("copyId", copyId);
            return "librarian/create-loan";
        }
        BookCopy copy = copyOpt.get();
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
                model.addAttribute("errorMsg", "Bản sao sách này hiện đang được đặt giữ chỗ bởi một Bạn đọc khác!");
                model.addAttribute("patronId", patronId);
                model.addAttribute("copyId", copyId);
                return "librarian/create-loan";
            }
        } else if (!"Available".equalsIgnoreCase(copy.getCopyStatus())) {
            model.addAttribute("errorMsg", "Bản sao sách này hiện không sẵn sàng để mượn (Trạng thái hiện tại: " + copy.getCopyStatus() + ")!");
            model.addAttribute("patronId", patronId);
            model.addAttribute("copyId", copyId);
            return "librarian/create-loan";
        }

        // Determine loan duration
        List<String> patronRoles = userRepository.findRolesByUserId(patronId);
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


        // Create BorrowTicket
        BorrowTicket ticket = new BorrowTicket();
        ticket.setPatron(patron);
        ticket.setLibrarian(librarian);
        ticket.setCampus(librarianCampus);
        ticket.setCreatedAt(LocalDateTime.now());
        ticket.setNote("Được tạo bởi thủ thư tại quầy");
        ticket = borrowTicketRepository.save(ticket);

        // Create BorrowTicketDetail
        BorrowTicketDetail detail = new BorrowTicketDetail();
        detail.setBorrowTicket(ticket);
        detail.setBookCopy(copy);
        detail.setDueDate(LocalDateTime.now().plusDays(loanDays));
        detail.setRenewalCount(0);
        detail.setStatus("Borrowing");
        detail.setReturnDate(null);
        borrowTicketDetailRepository.save(detail);

        // Update BookCopy status
        copy.setCopyStatus("Borrowed");
        bookCopyRepository.save(copy);

        // Update Reservation status if checkout for a reserved copy
        if (matchedReservation != null) {
            matchedReservation.setStatus("Completed");
            reservationRepository.save(matchedReservation);
        }

        // Send email confirmation to patron
        String formattedDueDate = detail.getDueDate().format(DateTimeFormatter.ofPattern("HH:mm 'ngày' dd/MM/yyyy"));
        emailService.sendLoanConfirmation(
                patron.getEmail(),
                patron.getFullName(),
                copy.getBook().getTitle(),
                copy.getCopyId(),
                formattedDueDate
        );

        redirectAttrs.addFlashAttribute("successMsg", "Tạo đơn mượn sách thành công! Bạn đọc " + patron.getFullName() + " đã mượn bản sao " + copyId + " (Hạn trả: " + detail.getDueDate().toLocalDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + ").");
        return "redirect:/librarian/create-loan";
    }
    // ── 4. ACQUISITION DASHBOARD ──
    @GetMapping("/acquisition/dashboard")
    public String dashboard(@RequestParam(required = false) String status,
                            @RequestParam(required = false) String search,
                            @RequestParam(required = false) String patronRole,
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
        long orderedCount  = (librarianCampusId != null)
                ? materialRequestRepository.countByStatusAndPatronCampusId("Ordered",  librarianCampusId)
                : materialRequestRepository.countByStatus("Ordered");
        long arrivedCount  = (librarianCampusId != null)
                ? materialRequestRepository.countByStatusAndPatronCampusId("Arrived",  librarianCampusId)
                : materialRequestRepository.countByStatus("Arrived");

        // Request list scoped to this librarian's campus
        List<MaterialRequest> requests = (librarianCampusId != null)
                ? materialRequestRepository.findByStatusAndSearchTermAndCampusId(status, search, librarianCampusId)
                : materialRequestRepository.findByStatusAndSearchTerm(status, search);

        // Filter by Patron Role (Student / Lecturer)
        if (patronRole != null && !patronRole.isEmpty()) {
            if ("Student".equalsIgnoreCase(patronRole)) {
                requests = requests.stream()
                        .filter(r -> r.getPatron() != null && 
                                (r.getPatron().getRole() == null || Integer.valueOf(1).equals(r.getPatron().getRole().getRoleId())))
                        .toList();
            } else if ("Lecturer".equalsIgnoreCase(patronRole)) {
                requests = requests.stream()
                        .filter(r -> r.getPatron() != null && 
                                r.getPatron().getRole() != null && Integer.valueOf(2).equals(r.getPatron().getRole().getRoleId()))
                        .toList();
            }
        }
        

        model.addAttribute("pendingCount",       pendingCount);
        model.addAttribute("approvedCount",      approvedCount);
        model.addAttribute("rejectedCount",      rejectedCount);
        model.addAttribute("orderedCount",       orderedCount);
        model.addAttribute("arrivedCount",       arrivedCount);
        model.addAttribute("requests",           requests);
        model.addAttribute("currentStatus",      status);
        model.addAttribute("currentSearch",      search);
        model.addAttribute("currentPatronRole",  patronRole);
        model.addAttribute("librarianCampusId",  librarianCampusId);

        return "acquisition/dashboard";
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
