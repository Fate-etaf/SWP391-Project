package com.swp5.library_management.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.swp5.library_management.dto.ReservationResultDTO;
import com.swp5.library_management.repository.BookRepository;
import com.swp5.library_management.repository.CampusRepository;
import com.swp5.library_management.service.ReservationService;

/**
 * Controller xử lý Use Case UCR06 – Reserve Book Online.
 *
 * Các endpoint:
 *   GET  /reservations              → Trang quản lý đặt chỗ cá nhân (Alt 1)
 *   GET  /reservations/reserve      → Form đặt giữ chỗ cho 1 cuốn sách
 *   POST /reservations/reserve      → Xử lý Normal Flow / Exc 1,2,3,4
 *   POST /reservations/cancel/{id}  → Alt 1: Hủy đơn đặt chỗ
 *   POST /reservations/waitlist     → Exc 3/4: Đăng ký xếp hàng chờ
 *   POST /reservations/waitlist/cancel/{id} → Alt 2: Hủy xếp hàng chờ
 */
@Controller
@RequestMapping("/reservations")
public class ReservationController {

    private final ReservationService reservationService;
    private final CampusRepository   campusRepository;
    private final BookRepository     bookRepository;

    public ReservationController(ReservationService reservationService,
                                 CampusRepository campusRepository,
                                 BookRepository bookRepository) {
        this.reservationService = reservationService;
        this.campusRepository   = campusRepository;
        this.bookRepository     = bookRepository;
    }

    // ── Hàm tiện ích: Lấy patronId từ Session, redirect về login nếu chưa đăng nhập ──

    /**
     * Lấy userId của người dùng đang đăng nhập từ HttpSession.
     * Session key "loggedInUserId" được ghi bởi UserController.loginUser() sau khi xác thực thành công.
     *
     * @return userId dạng String, hoặc null nếu chưa đăng nhập.
     */
    private String getPatronIdFromSession(HttpSession session) {
        return (String) session.getAttribute("loggedInUserId");
    }

    /**
     * Lấy campusId của người dùng đang đăng nhập từ HttpSession.
     *
     * @return campusId dạng Integer, hoặc null nếu chưa đăng nhập.
     */
    private Integer getCampusIdFromSession(HttpSession session) {
        return (Integer) session.getAttribute("loggedInCampusId");
    }

    // ── GET /reservations → Trang cá nhân: danh sách đặt chỗ ────────────────

    @GetMapping({"", "/"})
    public String myReservations(HttpSession session, Model model, RedirectAttributes redirectAttrs) {
        String patronId = getPatronIdFromSession(session);
        if (patronId == null) {
            redirectAttrs.addFlashAttribute("errorMsg", "Vui lòng đăng nhập để xem danh sách đặt chỗ.");
            return "redirect:/login";
        }

        var reservations = reservationService.getMyReservations(patronId);
        var waitlists    = reservationService.getMyWaitlists(patronId);

        model.addAttribute("reservations", reservations);
        model.addAttribute("waitlists",    waitlists);
        model.addAttribute("patronId",     patronId);
        return "reservation/my-reservations";
    }

    // ── GET /reservations/reserve?bookId=X → Form chọn campus & đặt chỗ ─────

    @GetMapping("/reserve")
    public String showReserveForm(@RequestParam(required = false) Integer bookId,
                                  HttpSession session,
                                  Model model,
                                  RedirectAttributes redirectAttrs) {
        String patronId = getPatronIdFromSession(session);
        if (patronId == null) {
            redirectAttrs.addFlashAttribute("errorMsg", "Vui lòng đăng nhập để đặt sách.");
            return "redirect:/login";
        }

        if (bookId == null) {
            return "redirect:/books";
        }

        var bookOpt = bookRepository.findById(bookId);
        if (bookOpt.isEmpty()) {
            return "redirect:/books";
        }

        model.addAttribute("book",     bookOpt.get());
        model.addAttribute("campuses", campusRepository.findAll());
        model.addAttribute("patronId", patronId);
        return "reservation/reserve-book";
    }

    // ── POST /reservations/reserve → Xử lý Normal Flow ───────────────────────

    @PostMapping("/reserve")
    public String processReserve(@RequestParam Integer bookId,
                                 @RequestParam Integer campusId,
                                 HttpSession session,
                                 RedirectAttributes redirectAttrs) {

        String patronId = getPatronIdFromSession(session);
        if (patronId == null) {
            redirectAttrs.addFlashAttribute("errorMsg", "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.");
            return "redirect:/login";
        }

        ReservationResultDTO result = reservationService.reserveBook(patronId, bookId, campusId);

        if ("RESERVED".equals(result.getResultType())) {
            // Thành công → chuyển về trang cá nhân với flash message thành công
            redirectAttrs.addFlashAttribute("successMsg",      result.getMessage());
            redirectAttrs.addFlashAttribute("expirationDate",  result.getExpirationDate());
            return "redirect:/reservations";

        } else if ("NO_COPY".equals(result.getResultType())) {
            // Exc 3/4: Hết sách → chuyển đến trang đăng ký waitlist
            redirectAttrs.addFlashAttribute("noCopyMsg",  result.getMessage());
            redirectAttrs.addFlashAttribute("bookId",     bookId);
            redirectAttrs.addFlashAttribute("campusId",   campusId);
            return "redirect:/reservations/reserve?bookId=" + bookId + "&showWaitlist=true&campusId=" + campusId;

        } else {
            // Exc 1/2: Lỗi tài khoản hoặc vượt giới hạn → quay lại form
            redirectAttrs.addFlashAttribute("errorMsg", result.getMessage());
            return "redirect:/reservations/reserve?bookId=" + bookId;
        }
    }

    // ── POST /reservations/cancel/{id} → Alt 1: Hủy đặt chỗ ─────────────────

    @PostMapping("/cancel/{reservationId}")
    public String cancelReservation(@PathVariable Integer reservationId,
                                    HttpSession session,
                                    RedirectAttributes redirectAttrs) {

        String patronId = getPatronIdFromSession(session);
        if (patronId == null) {
            redirectAttrs.addFlashAttribute("errorMsg", "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.");
            return "redirect:/login";
        }

        try {
            reservationService.cancelReservation(patronId, reservationId);
            redirectAttrs.addFlashAttribute("successMsg",
                    "Đơn đặt giữ chỗ #" + reservationId + " đã được hủy thành công.");
        } catch (IllegalStateException | SecurityException e) {
            redirectAttrs.addFlashAttribute("errorMsg", e.getMessage());
        }

        return "redirect:/reservations";
    }

    // ── POST /reservations/waitlist → Exc 3/4: Đăng ký hàng chờ ─────────────

    @PostMapping("/waitlist")
    public String processWaitlist(@RequestParam Integer bookId,
                                  @RequestParam Integer campusId,
                                  HttpSession session,
                                  RedirectAttributes redirectAttrs) {

        String patronId = getPatronIdFromSession(session);
        if (patronId == null) {
            redirectAttrs.addFlashAttribute("errorMsg", "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.");
            return "redirect:/login";
        }

        ReservationResultDTO result = reservationService.joinWaitlist(patronId, bookId, campusId);

        if (result.isSuccess()) {
            redirectAttrs.addFlashAttribute("successMsg", result.getMessage());
            return "redirect:/reservations";
        } else {
            redirectAttrs.addFlashAttribute("errorMsg", result.getMessage());
            return "redirect:/reservations/reserve?bookId=" + bookId;
        }
    }

    // ── POST /reservations/waitlist/cancel/{id} → Alt 2: Hủy hàng chờ ────────

    @PostMapping("/waitlist/cancel/{waitlistId}")
    public String cancelWaitlist(@PathVariable Integer waitlistId,
                                 HttpSession session,
                                 RedirectAttributes redirectAttrs) {

        String patronId = getPatronIdFromSession(session);
        if (patronId == null) {
            redirectAttrs.addFlashAttribute("errorMsg", "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.");
            return "redirect:/login";
        }

        try {
            reservationService.cancelWaitlist(patronId, waitlistId);
            redirectAttrs.addFlashAttribute("successMsg", "Đăng ký xếp hàng chờ đã được hủy thành công.");
        } catch (IllegalStateException | SecurityException e) {
            redirectAttrs.addFlashAttribute("errorMsg", e.getMessage());
        }

        return "redirect:/reservations";
    }

    // ── Xử lý lỗi toàn cục cho Controller này ────────────────────────────────

    @ExceptionHandler(jakarta.persistence.EntityNotFoundException.class)
    public String handleEntityNotFoundException(jakarta.persistence.EntityNotFoundException ex,
                                                RedirectAttributes redirectAttrs) {
        redirectAttrs.addFlashAttribute("errorMsg", "Lỗi: " + ex.getMessage());
        return "redirect:/books";
    }
}
