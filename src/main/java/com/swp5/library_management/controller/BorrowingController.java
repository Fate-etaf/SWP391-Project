package com.swp5.library_management.controller;

import com.swp5.library_management.entity.BorrowTicketDetail;
import com.swp5.library_management.service.ViolationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/borrowing")
public class BorrowingController {

    private static final int PAGE_SIZE = 10;

    private final ViolationService violationService;

    // ---------------------------------------------------------------
    // Trang: Sách đang được mượn (chưa trả)
    // ---------------------------------------------------------------
    @GetMapping
    public String showBorrowingList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) String borrowerId,
            @RequestParam(required = false) String bookTitle,
            Model model) {

        List<BorrowTicketDetail> all = violationService.getBorrowingBooks();
        List<BorrowTicketDetail> filtered = new ArrayList<>();

        for (BorrowTicketDetail d : all) {
            if (borrowerId != null && !borrowerId.isBlank()) {
                String pid = d.getBorrowTicket() != null && d.getBorrowTicket().getPatron() != null
                        ? d.getBorrowTicket().getPatron().getUserId()
                        : "";
                if (!pid.equalsIgnoreCase(borrowerId.trim()))
                    continue;
            }
            if (bookTitle != null && !bookTitle.isBlank()) {
                String title = d.getBookCopy() != null && d.getBookCopy().getBook() != null
                        ? d.getBookCopy().getBook().getTitle()
                        : "";
                if (!title.toLowerCase().contains(bookTitle.toLowerCase().trim()))
                    continue;
            }
            filtered.add(d);
        }

        int totalPages = (int) Math.ceil((double) filtered.size() / PAGE_SIZE);
        int safePage = Math.max(0, Math.min(page, Math.max(totalPages - 1, 0)));
        List<BorrowTicketDetail> paged = paginate(filtered, safePage, PAGE_SIZE);

        model.addAttribute("borrowingItems", paged);
        model.addAttribute("currentPage", safePage);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("borrowerId", borrowerId);
        model.addAttribute("bookTitle", bookTitle);
        return "violations/borrowing-list";
    }

    // ---------------------------------------------------------------
    // Trả sách
    // ---------------------------------------------------------------
    @PostMapping("/return/{id}")
    public String returnBook(@PathVariable("id") Integer id) {
        violationService.returnBook(id);
        return "redirect:/borrowing";
    }

    @PostMapping("/create-lost/{id}")
    public String createLostFine(@PathVariable("id") Integer id) {
        violationService.createLostBookFine(id);
        return "redirect:/borrowing";
    }

    @PostMapping("/create-damaged/{id}")
    public String createDamagedFine(@PathVariable("id") Integer id) {
        violationService.createDamagedBookFine(id);
        return "redirect:/borrowing";
    }

    // ---------------------------------------------------------------
    // Desk Return Center: Quét Barcode trả sách
    // ---------------------------------------------------------------
    @GetMapping("/return-center")
    public String showReturnCenter(Model model) {
        model.addAttribute("borrowingItems", violationService.getBorrowingBooks());
        return "violations/return-center";
    }

    @PostMapping("/return-barcode")
    public String returnBarcode(@RequestParam("copyId") String copyId, RedirectAttributes ra) {
        try {
            BorrowTicketDetail detail = violationService.returnByCopyId(copyId.trim());
            ra.addFlashAttribute("successMsg", "Đã trả sách thành công: "
                    + detail.getBookCopy().getBook().getTitle()
                    + " (Copy ID: " + copyId + ")");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMsg", "Lỗi: " + e.getMessage());
        }
        return "redirect:/borrowing/return-center";
    }

    // ---------------------------------------------------------------
    // Helper
    // ---------------------------------------------------------------
    private <T> List<T> paginate(List<T> items, int page, int size) {
        if (items.isEmpty())
            return items;
        int totalPages = (int) Math.ceil((double) items.size() / size);
        int safePage = Math.max(0, Math.min(page, Math.max(totalPages - 1, 0)));
        int from = safePage * size;
        int to = Math.min(from + size, items.size());
        return items.subList(from, to);
    }
}
