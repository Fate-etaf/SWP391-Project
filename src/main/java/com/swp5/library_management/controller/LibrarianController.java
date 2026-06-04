package com.swp5.library_management.controller;

import com.swp5.library_management.entity.*;
import com.swp5.library_management.repository.*;
import com.swp5.library_management.service.SystemConfigService;
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
    private final SystemConfigService systemConfigService;

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
        if (!"Available".equalsIgnoreCase(copy.getCopyStatus())) {
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

        redirectAttrs.addFlashAttribute("successMsg", "Tạo đơn mượn sách thành công! Bạn đọc " + patron.getFullName() + " đã mượn bản sao " + copyId + " (Hạn trả: " + detail.getDueDate().toLocalDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + ").");
        return "redirect:/librarian/create-loan";
    }
}
