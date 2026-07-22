package com.swp5.library_management.service;

import com.swp5.library_management.entity.BookCopy;
import com.swp5.library_management.entity.BorrowTicketDetail;
import com.swp5.library_management.entity.FineInvoice;
import com.swp5.library_management.repository.BookCopyRepository;
import com.swp5.library_management.repository.BorrowTicketDetailRepository;
import com.swp5.library_management.repository.FineInvoiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ViolationServiceImpl implements ViolationService {

    private static final BigDecimal OVERDUE_DAILY_FINE = BigDecimal.valueOf(5000);
    private static final BigDecimal LOST_FIXED_FINE = BigDecimal.valueOf(200000);
    private static final BigDecimal DAMAGED_FIXED_FINE = BigDecimal.valueOf(50000);

    private final BorrowTicketDetailRepository borrowTicketDetailRepository;
    private final FineInvoiceRepository fineInvoiceRepository;
    private final BookCopyRepository bookCopyRepository;
    private final ReservationService reservationService;
    private final SystemConfigService systemConfigService;

    @Override
    @Transactional(readOnly = true)
    public List<BorrowTicketDetail> getBorrowingBooks() {
        return borrowTicketDetailRepository.findCurrentlyBorrowing();
    }

    @Override
    @Transactional(readOnly = true)
    public List<BorrowTicketDetail> getReturnedOverdueBooks() {
        return borrowTicketDetailRepository.findReturnedOverdue();
    }

    @Override
    public long calculateOverdueDays(LocalDate dueDate) {
        if (dueDate == null) {
            return 0L;
        }
        long days = ChronoUnit.DAYS.between(dueDate, LocalDate.now());
        return Math.max(days, 0L);
    }

    @Override
    public void returnBook(Integer borrowTicketDetailId) {
        BorrowTicketDetail detail = getDetailOrThrow(borrowTicketDetailId);
        if (detail.getReturnDate() == null) {
            detail.setReturnDate(LocalDateTime.now());
        }
        detail.setStatus("Returned");
        borrowTicketDetailRepository.save(detail);
        BookCopy copy = detail.getBookCopy();
        if (copy != null) {
            boolean assigned = reservationService.processWaitlistForReturnedBook(copy);
            if (!assigned) {
                markCopyStatus(copy, "Available", copy.getConditionStatus());
            }
        }
    }

    @Override
    public FineInvoice createOverdueFine(Integer borrowTicketDetailId) {
        BorrowTicketDetail detail = getDetailOrThrow(borrowTicketDetailId);
        FineInvoice fine = fineInvoiceRepository
                .findByTicketDetailAndViolationType(detail, "OVERDUE")
                .orElseGet(() -> buildOverdueFine(detail));
        if (detail.getReturnDate() == null) {
            detail.setReturnDate(LocalDateTime.now());
        }
        detail.setStatus("Returned");
        borrowTicketDetailRepository.save(detail);
        BookCopy copy = detail.getBookCopy();
        if (copy != null) {
            markCopyStatus(copy, "Available", copy.getConditionStatus());
        }
        fine.setPaidStatus("Paid");
        fine.setRemainingAmount(BigDecimal.ZERO);
        fine.setPaidAt(LocalDateTime.now());
        return fineInvoiceRepository.save(fine);
    }

    @Override
    public FineInvoice createLostBookFine(Integer borrowTicketDetailId) {
        BorrowTicketDetail detail = getDetailOrThrow(borrowTicketDetailId);
        markCopyStatus(detail.getBookCopy(), "Maintenance", "Lost");
        updateTicketDetailStatus(detail, "Lost");

        return fineInvoiceRepository
                .findByTicketDetailAndViolationType(detail, "LOST")
                .orElseGet(() -> buildFixedFine(detail, "LOST", getLostFineAmount(detail), "Lost book"));
    }

    @Override
    public FineInvoice createDamagedBookFine(Integer borrowTicketDetailId) {
        BorrowTicketDetail detail = getDetailOrThrow(borrowTicketDetailId);
        markCopyStatus(detail.getBookCopy(), "Maintenance", "Damaged");
        updateTicketDetailStatus(detail, "Damaged");

        return fineInvoiceRepository
                .findByTicketDetailAndViolationType(detail, "DAMAGED")
                .orElseGet(() -> buildDamagedFine(detail));
    }

    private BorrowTicketDetail getDetailOrThrow(Integer id) {
        return borrowTicketDetailRepository
                .findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Borrow ticket detail not found: " + id));
    }

    private FineInvoice buildOverdueFine(BorrowTicketDetail detail) {
        LocalDate dueDate = detail.getDueDate() != null ? detail.getDueDate().toLocalDate() : null;
        long overdueDays = calculateOverdueDays(dueDate);
        if (overdueDays <= 0) {
            throw new IllegalStateException("Borrow ticket detail is not overdue.");
        }

        BigDecimal finePerDay = BigDecimal.valueOf(systemConfigService.getIntConfig("FINE_PER_DAY", 5000));
        BigDecimal amount = finePerDay.multiply(BigDecimal.valueOf(overdueDays));
        return buildFine(detail, amount, "OVERDUE", "Overdue " + overdueDays + " day(s)");
    }

    private FineInvoice buildDamagedFine(BorrowTicketDetail detail) {
        BigDecimal amount = getDamagedFineAmount(detail);
        return buildFine(detail, amount, "DAMAGED", "Damaged book");
    }

    private FineInvoice buildFixedFine(BorrowTicketDetail detail, String violationType, BigDecimal amount,
            String reason) {
        return buildFine(detail, amount, violationType, reason);
    }

    private FineInvoice buildFine(BorrowTicketDetail detail, BigDecimal amount, String violationType, String reason) {
        FineInvoice fine = new FineInvoice();
        fine.setTicketDetail(detail);
        fine.setPatron(detail.getBorrowTicket() != null ? detail.getBorrowTicket().getPatron() : null);
        fine.setFineAmount(amount);
        fine.setRemainingAmount(amount);
        fine.setViolationType(violationType);
        fine.setReason(reason);
        fine.setCreatedAt(LocalDateTime.now());
        fine.setPaidStatus("UNPAID");
        return fineInvoiceRepository.save(fine);
    }

    private BigDecimal getLostFineAmount(BorrowTicketDetail detail) {
        // Book price is not modeled in the current entity, so use fixed amount.
        return LOST_FIXED_FINE;
    }

    private BigDecimal getDamagedFineAmount(BorrowTicketDetail detail) {
        // Book price is not available in the entity, so use fixed amount.
        return DAMAGED_FIXED_FINE;
    }

    private void markCopyStatus(BookCopy copy, String copyStatus, String conditionStatus) {
        if (copy == null) {
            return;
        }
        copy.setCopyStatus(copyStatus);
        copy.setConditionStatus(conditionStatus);
        bookCopyRepository.save(copy);
    }

    private void updateTicketDetailStatus(BorrowTicketDetail detail, String status) {
        detail.setStatus(status);
        borrowTicketDetailRepository.save(detail);
    }

    @Override
    public BorrowTicketDetail returnByCopyId(String copyId) {
        List<BorrowTicketDetail> actives = borrowTicketDetailRepository.findActiveByCopyId(copyId.trim());
        if (actives.isEmpty()) {
            throw new IllegalArgumentException("Không tìm thấy lượt mượn đang hoạt động cho mã sách: " + copyId);
        }
        // Thường mỗi copy chỉ được mượn bởi 1 người tại 1 thời điểm
        BorrowTicketDetail detail = actives.get(0);
        returnBook(detail.getTicketDetailId());
        return detail;
    }
}
