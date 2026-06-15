package com.swp5.library_management.reader.controller;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.swp5.library_management.reader.dto.BorrowingHistoryDTO;
import com.swp5.library_management.reader.service.BorrowingService;

import java.util.List;

@Controller
@RequestMapping("/borrowing-history")
@RequiredArgsConstructor
public class BorrowingController {

    private final BorrowingService borrowingService;

    @GetMapping
    public String showBorrowingHistory(HttpSession session, Model model, RedirectAttributes redirectAttrs) {
        String patronId = (String) session.getAttribute("loggedInUserId");
        if (patronId == null) {
            redirectAttrs.addFlashAttribute("errorMsg", "Vui lòng đăng nhập để xem lịch sử mượn trả.");
            return "redirect:/login";
        }

        List<BorrowingHistoryDTO> historyList = borrowingService.getBorrowingHistory(patronId);
        model.addAttribute("historyList", historyList);
        model.addAttribute("patronId", patronId);

        return "borrowing/history";
    }
}
