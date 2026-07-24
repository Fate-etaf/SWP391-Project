package com.swp5.library_management.controller;

import com.swp5.library_management.entity.BorrowTicketDetail;
import com.swp5.library_management.entity.FineInvoice;
import com.swp5.library_management.repository.FineInvoiceRepository;
import com.swp5.library_management.service.ViolationService;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/violations")
public class ViolationController {

    private static final BigDecimal OVERDUE_DAILY_FINE = BigDecimal.valueOf(5000);
    private static final int PAGE_SIZE = 10;

    private final ViolationService violationService;
    private final FineInvoiceRepository fineInvoiceRepository;

    private boolean isNotLibrarian(HttpSession session) {
        Boolean isLibrarian = (Boolean) session.getAttribute("isLibrarian");
        return isLibrarian == null || !isLibrarian;
    }

    // ---------------------------------------------------------------
    // Trang: Sách đã trả quá hạn
    // ---------------------------------------------------------------
    @GetMapping("/overdue")
    public String showOverdueList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) BigDecimal minFine,
            @RequestParam(required = false) BigDecimal maxFine,
            @RequestParam(required = false) String paidStatus,
            @RequestParam(required = false) String borrowerId,
            @RequestParam(required = false) Long minOverdueDays,
            @RequestParam(required = false) Long maxOverdueDays,
            HttpSession session,
            RedirectAttributes redirectAttrs,
            Model model) {
        if (isNotLibrarian(session)) {
            redirectAttrs.addFlashAttribute("errorMsg", "Quyền truy cập bị từ chối. Vui lòng đăng nhập bằng tài khoản Thủ thư.");
            return "redirect:/login";
        }
        List<BorrowTicketDetail> overdueDetails = violationService.getReturnedOverdueBooks();
        List<OverdueItem> items = new ArrayList<>();

        for (BorrowTicketDetail detail : overdueDetails) {
            LocalDate dueDate = detail.getDueDate() != null ? detail.getDueDate().toLocalDate() : null;
            LocalDate returnDate = detail.getReturnDate() != null ? detail.getReturnDate().toLocalDate() : null;
            long overdueDays = calculateOverdueDays(dueDate, returnDate);

            FineInvoice fine = fineInvoiceRepository
                    .findByTicketDetailAndViolationType(detail, "OVERDUE")
                    .orElse(null);

            BigDecimal fineAmount = OVERDUE_DAILY_FINE.multiply(BigDecimal.valueOf(overdueDays));
            if (fine != null) {
                fine.setFineAmount(fineAmount);
                if (fine.getPaidStatus() == null || !"PAID".equalsIgnoreCase(fine.getPaidStatus())) {
                    fine.setRemainingAmount(fineAmount);
                }
                fineInvoiceRepository.save(fine);
            }

            String fineStatus = (fine != null && fine.getPaidStatus() != null)
                    ? fine.getPaidStatus().toUpperCase()
                    : (fine != null ? "UNPAID" : "NOT_CREATED");

            OverdueItem item = new OverdueItem(detail, overdueDays, fineAmount, fineStatus);
            if (matchesFilter(item, minFine, maxFine, paidStatus, borrowerId, minOverdueDays, maxOverdueDays)) {
                items.add(item);
            }
        }

        // Sắp xếp: Chưa trả (UNPAID/NOT_CREATED) lên trên, Đã trả (PAID) xuống dưới
        items.sort(Comparator.comparingInt(item -> "PAID".equalsIgnoreCase(item.getFineStatus()) ? 1 : 0));

        int totalPages = (int) Math.ceil((double) items.size() / PAGE_SIZE);
        List<OverdueItem> pagedItems = paginate(items, page, PAGE_SIZE);

        model.addAttribute("overdueItems", pagedItems);
        model.addAttribute("currentPage", Math.max(0, Math.min(page, Math.max(totalPages - 1, 0))));
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("minFine", minFine);
        model.addAttribute("maxFine", maxFine);
        model.addAttribute("paidStatus", paidStatus);
        model.addAttribute("borrowerId", borrowerId);
        model.addAttribute("minOverdueDays", minOverdueDays);
        model.addAttribute("maxOverdueDays", maxOverdueDays);
        return "violations/overdue-list";
    }

    @PostMapping("/create-overdue/{id}")
    public String createOverdueFine(@PathVariable("id") Integer id, HttpSession session, RedirectAttributes redirectAttrs) {
        if (isNotLibrarian(session)) {
            redirectAttrs.addFlashAttribute("errorMsg", "Quyền truy cập bị từ chối. Vui lòng đăng nhập bằng tài khoản Thủ thư.");
            return "redirect:/login";
        }
        try {
            String librarianId = (String) session.getAttribute("loggedInUserId");
            // conditionStatus = null: giữ nguyên tình trạng sách khi tạo phiếu phạt thủ công
            violationService.createOverdueFine(id, null, librarianId);
            redirectAttrs.addFlashAttribute("successMsg", "Tạo phiếu phạt quá hạn thành công!");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("errorMsg", "Lỗi: " + e.getMessage());
        }
        return "redirect:/violations/overdue";
    }

    // ---------------------------------------------------------------
    // Inner class
    // ---------------------------------------------------------------
    public static class OverdueItem {
        private final BorrowTicketDetail detail;
        private final long overdueDays;
        private final BigDecimal fineAmount;
        private final String fineStatus;

        public OverdueItem(BorrowTicketDetail detail, long overdueDays, BigDecimal fineAmount, String fineStatus) {
            this.detail = detail;
            this.overdueDays = overdueDays;
            this.fineAmount = fineAmount;
            this.fineStatus = fineStatus;
        }

        public BorrowTicketDetail getDetail() {
            return detail;
        }

        public long getOverdueDays() {
            return overdueDays;
        }

        public BigDecimal getFineAmount() {
            return fineAmount;
        }

        public String getFineStatus() {
            return fineStatus;
        }
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------
    private boolean matchesFilter(OverdueItem item, BigDecimal minFine, BigDecimal maxFine,
            String paidStatus, String borrowerId,
            Long minOverdueDays, Long maxOverdueDays) {
        if (minFine != null && item.getFineAmount().compareTo(minFine) < 0)
            return false;
        if (maxFine != null && item.getFineAmount().compareTo(maxFine) > 0)
            return false;
        if (minOverdueDays != null && item.getOverdueDays() < minOverdueDays)
            return false;
        if (maxOverdueDays != null && item.getOverdueDays() > maxOverdueDays)
            return false;
        if (paidStatus != null && !paidStatus.isBlank() && !"ALL".equalsIgnoreCase(paidStatus)) {
            return paidStatus.equalsIgnoreCase(item.getFineStatus());
        }
        if (borrowerId != null && !borrowerId.isBlank()) {
            String actual = null;
            if (item.getDetail().getBorrowTicket() != null && item.getDetail().getBorrowTicket().getPatron() != null) {
                actual = item.getDetail().getBorrowTicket().getPatron().getUserId();
            }
            if (actual == null || !actual.equalsIgnoreCase(borrowerId.trim()))
                return false;
        }
        return true;
    }

    private <T> List<T> paginate(List<T> items, int page, int size) {
        if (items.isEmpty())
            return items;
        int totalPages = (int) Math.ceil((double) items.size() / size);
        int safePage = Math.max(0, Math.min(page, Math.max(totalPages - 1, 0)));
        int from = safePage * size;
        int to = Math.min(from + size, items.size());
        return items.subList(from, to);
    }

    private long calculateOverdueDays(LocalDate dueDate, LocalDate endDate) {
        if (dueDate == null || endDate == null)
            return 0L;
        return Math.max(ChronoUnit.DAYS.between(dueDate, endDate), 0L);
    }
}
