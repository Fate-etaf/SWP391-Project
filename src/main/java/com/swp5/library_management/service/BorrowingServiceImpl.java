package com.swp5.library_management.service;

import com.swp5.library_management.dto.BorrowingHistoryDTO;
import com.swp5.library_management.entity.Book;
import com.swp5.library_management.entity.BookCopy;
import com.swp5.library_management.entity.BorrowTicketDetail;
import com.swp5.library_management.repository.BorrowTicketDetailRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BorrowingServiceImpl implements BorrowingService {

    private final BorrowTicketDetailRepository borrowTicketDetailRepository;
    private final com.swp5.library_management.repository.WaitlistRepository waitlistRepository;
    private final com.swp5.library_management.service.SystemConfigService systemConfigService;

    private static final String[] COVER_COLORS = {
        "from-slate-700 to-slate-900",
        "from-blue-700 to-indigo-900",
        "from-emerald-700 to-teal-900",
        "from-red-700 to-rose-900",
        "from-violet-700 to-purple-900",
        "from-amber-600 to-orange-900",
        "from-cyan-700 to-blue-900",
        "from-fuchsia-700 to-pink-900"
    };

    @Override
    public List<BorrowingHistoryDTO> getBorrowingHistory(String patronId) {
        List<BorrowTicketDetail> details = borrowTicketDetailRepository.findHistoryByPatronId(patronId);
        List<BorrowingHistoryDTO> list = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (BorrowTicketDetail detail : details) {
            BookCopy copy = detail.getBookCopy();
            Book book = copy != null ? copy.getBook() : null;

            String resolvedStatus = "Borrowing";
            if (detail.getStatus() != null && "Lost".equalsIgnoreCase(detail.getStatus())) {
                resolvedStatus = "Lost";
            } else if (detail.getStatus() != null && "Damaged".equalsIgnoreCase(detail.getStatus())) {
                resolvedStatus = "Damaged";
            } else if (detail.getReturnDate() != null) {
                resolvedStatus = "Returned";
            } else if (detail.getDueDate() != null && detail.getDueDate().isBefore(now)) {
                resolvedStatus = "Overdue";
            }

            String authorNames = (book != null) ? book.getAuthorNames() : "Unknown Author";
            String title = (book != null) ? book.getTitle() : "Unknown Title";
            String coverUrl = (book != null) ? book.getCoverImageUrl() : null;
            Integer bookId = (book != null) ? book.getBookId() : null;
            String coverColor = (book != null) ? COVER_COLORS[book.getBookId() % COVER_COLORS.length] : COVER_COLORS[0];
            String returnCampusName = (detail.getReturnCampus() != null) ? detail.getReturnCampus().getCampusName() : null;

            BorrowingHistoryDTO dto = BorrowingHistoryDTO.builder()
                    .ticketDetailId(detail.getTicketDetailId())
                    .ticketId(detail.getBorrowTicket() != null ? detail.getBorrowTicket().getTicketId() : null)
                    .bookId(bookId)
                    .bookTitle(title)
                    .coverImageUrl(coverUrl)
                    .coverColor(coverColor)
                    .authorNames(authorNames)
                    .copyId(copy != null ? copy.getCopyId() : null)
                    .borrowDate(detail.getBorrowTicket() != null ? detail.getBorrowTicket().getCreatedAt() : null)
                    .dueDate(detail.getDueDate())
                    .returnDate(detail.getReturnDate())
                    .renewalCount(detail.getRenewalCount())
                    .status(resolvedStatus)
                    .returnCampusName(returnCampusName)
                    .build();

            list.add(dto);
        }

        return list;
    }

    @Override
    @Transactional
    public com.swp5.library_management.dto.ReservationResultDTO renewBook(String patronId, Integer ticketDetailId) {
        BorrowTicketDetail detail = borrowTicketDetailRepository.findById(ticketDetailId)
                .orElse(null);

        if (detail == null || !detail.getBorrowTicket().getPatron().getUserId().equals(patronId)) {
            return com.swp5.library_management.dto.ReservationResultDTO.builder()
                    .success(false).message("Không tìm thấy thông tin lượt mượn sách hợp lệ.")
                    .build();
        }

        // R1: Trạng thái sách
        if (detail.getReturnDate() != null || (detail.getStatus() != null && !detail.getStatus().equals("Borrowing"))) {
            return com.swp5.library_management.dto.ReservationResultDTO.builder()
                    .success(false).message("Sách đã được trả hoặc không ở trạng thái đang mượn.")
                    .build();
        }

        if (detail.getDueDate() != null && detail.getDueDate().isBefore(LocalDateTime.now())) {
            return com.swp5.library_management.dto.ReservationResultDTO.builder()
                    .success(false).message("Sách đã quá hạn, không thể gia hạn. Vui lòng đến thư viện nộp phạt.")
                    .build();
        }

        // R2: Số lần gia hạn tối đa (user requested 4)
        int maxRenewals = systemConfigService.getIntConfig("MAX_RENEWALS", 4);
        int currentRenewals = detail.getRenewalCount() != null ? detail.getRenewalCount() : 0;
        if (currentRenewals >= maxRenewals) {
            return com.swp5.library_management.dto.ReservationResultDTO.builder()
                    .success(false).message("Bạn đã đạt giới hạn gia hạn tối đa (" + maxRenewals + " lần) cho cuốn sách này.")
                    .build();
        }

        // R3: Trạng thái tài khoản (Account valid and no overdues)
        var patron = detail.getBorrowTicket().getPatron();
        if (Boolean.TRUE.equals(patron.getBorrowingLocked())) {
            return com.swp5.library_management.dto.ReservationResultDTO.builder()
                    .success(false).message("Tài khoản của bạn đang bị khóa, không thể gia hạn sách.")
                    .build();
        }
        int overdueCount = borrowTicketDetailRepository.countOverdueByPatronId(patronId, LocalDateTime.now());
        if (overdueCount > 0) {
            return com.swp5.library_management.dto.ReservationResultDTO.builder()
                    .success(false).message("Bạn đang có " + overdueCount + " cuốn sách quá hạn chưa trả, không thể gia hạn sách mới.")
                    .build();
        }

        // R4: Waitlist check
        Integer bookId = detail.getBookCopy().getBook().getBookId();
        long waitingCount = waitlistRepository.countByBookBookIdAndStatusIn(bookId, List.of("Waiting", "Notified"));
        if (waitingCount > 0) {
            return com.swp5.library_management.dto.ReservationResultDTO.builder()
                    .success(false).message("Không thể gia hạn vì đang có " + waitingCount + " độc giả khác xếp hàng chờ cuốn sách này.")
                    .build();
        }

        // R5: Thời điểm cho phép gia hạn (chỉ khi còn <= 3 ngày)
        if (detail.getDueDate() != null) {
            long daysUntilDue = java.time.temporal.ChronoUnit.DAYS.between(LocalDateTime.now(), detail.getDueDate());
            if (daysUntilDue > 3) {
                return com.swp5.library_management.dto.ReservationResultDTO.builder()
                        .success(false).message("Chỉ được phép gia hạn khi thời hạn trả sách còn dưới 3 ngày.")
                        .build();
            }
        }

        // R6: Thực thi
        int renewalDays = systemConfigService.getIntConfig("RENEWAL_DAYS", 7);
        detail.setDueDate(detail.getDueDate().plusDays(renewalDays));
        detail.setRenewalCount(currentRenewals + 1);
        borrowTicketDetailRepository.save(detail);

        return com.swp5.library_management.dto.ReservationResultDTO.builder()
                .success(true)
                .message("Gia hạn thành công! Hạn trả mới của bạn là " + 
                         detail.getDueDate().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")))
                .build();
    }
}
