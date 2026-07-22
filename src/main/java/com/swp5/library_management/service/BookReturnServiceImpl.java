package com.swp5.library_management.service;

import com.swp5.library_management.entity.BorrowTicketDetail;
import com.swp5.library_management.entity.FineInvoice;
import com.swp5.library_management.entity.User;
import com.swp5.library_management.repository.BorrowTicketDetailRepository;
import com.swp5.library_management.repository.FineInvoiceRepository;
import com.swp5.library_management.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class BookReturnServiceImpl implements BookReturnService {

    private final java.util.Map<Integer, String> activeQrLibrarians = new java.util.concurrent.ConcurrentHashMap<>();

    private final BorrowTicketDetailRepository borrowTicketDetailRepository;
    private final FineInvoiceRepository fineInvoiceRepository;
    private final UserRepository userRepository;
    private final ViolationService violationService;

    @Override
    @Transactional(readOnly = true)
    public List<BorrowTicketDetail> getCurrentlyBorrowing() {
        return borrowTicketDetailRepository.findCurrentlyBorrowing();
    }

    @Override
    @Transactional(readOnly = true)
    public List<BorrowTicketDetail> searchCurrentlyBorrowing(String title, String borrowerId) {
        String t = (title == null || title.isBlank()) ? null : title.trim();
        String b = (borrowerId == null || borrowerId.isBlank()) ? null : borrowerId.trim();
        return borrowTicketDetailRepository.searchCurrentlyBorrowing(t, b);
    }

    @Override
    @Transactional
    public Map<String, Object> checkScan(String copyId) {
        if (copyId == null || copyId.isBlank()) {
            throw new IllegalArgumentException("Mã sách không được để trống");
        }
        List<BorrowTicketDetail> actives = borrowTicketDetailRepository.findActiveByCopyId(copyId.trim());
        if (actives.isEmpty()) {
            throw new IllegalArgumentException("Không tìm thấy lượt mượn đang hoạt động cho mã sách: " + copyId);
        }

        BorrowTicketDetail detail = actives.get(0);

        LocalDateTime borrowDate = detail.getBorrowTicket() != null ? detail.getBorrowTicket().getCreatedAt() : null;
        String borrowDateStr = "—";
        if (borrowDate != null) {
            borrowDateStr = borrowDate.format(java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy"));
        }

        LocalDate dueDate = detail.getDueDate() != null ? detail.getDueDate().toLocalDate() : LocalDate.now();
        String dueDateStr = detail.getDueDate() != null
                ? detail.getDueDate().toLocalDate().format(java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy"))
                : "—";

        LocalDate now = LocalDate.now();
        long overdueDays = ChronoUnit.DAYS.between(dueDate, now);
        long positiveOverdueDays = Math.max(overdueDays, 0L);
        BigDecimal fineAmount = BigDecimal.valueOf(positiveOverdueDays * 5000);

        Integer fineId = null;
        if (positiveOverdueDays > 0) {
            fineId = detail.getTicketDetailId();
        }

        Map<String, Object> result = new HashMap<>();
        result.put("ticketDetailId", detail.getTicketDetailId());
        result.put("copyId", detail.getBookCopy() != null ? detail.getBookCopy().getCopyId() : "—");
        result.put("bookTitle",
                (detail.getBookCopy() != null && detail.getBookCopy().getBook() != null)
                        ? detail.getBookCopy().getBook().getTitle()
                        : "—");
        result.put("patronId",
                (detail.getBorrowTicket() != null && detail.getBorrowTicket().getPatron() != null)
                        ? detail.getBorrowTicket().getPatron().getUserId()
                        : "—");
        result.put("patronName",
                (detail.getBorrowTicket() != null && detail.getBorrowTicket().getPatron() != null)
                        ? detail.getBorrowTicket().getPatron().getFullName()
                        : "—");
        result.put("borrowDate", borrowDateStr);
        result.put("dueDate", dueDateStr);
        result.put("overdueDays", positiveOverdueDays);
        result.put("fineAmount", fineAmount);
        result.put("isOverdue", positiveOverdueDays > 0);
        result.put("fineId", fineId);
        BigDecimal bookPrice = violationService.getBookPriceByTicketDetailId(detail.getTicketDetailId());
        result.put("bookPrice", bookPrice);
        result.put("combinedAmount", fineAmount.add(bookPrice));
        result.put("conditionStatus",
                detail.getBookCopy() != null ? detail.getBookCopy().getConditionStatus() : "Fair");

        return result;
    }

    @Override
    public void processNormalReturn(Integer ticketDetailId, String conditionStatus) {
        violationService.returnBook(ticketDetailId, conditionStatus);
    }

    @Override
    public void processOverdueReturn(Integer ticketDetailId, String paymentMethod, String transactionCode,
            String librarianId, String conditionStatus) {
        FineInvoice fine = violationService.createOverdueFine(ticketDetailId, conditionStatus);
        if ("PayLater".equalsIgnoreCase(paymentMethod)) {
            fine.setPaidStatus("UNPAID");
            fine.setRemainingAmount(fine.getFineAmount());
            fine.setPaidAt(null);
            fine.setPaymentMethod(null);
            fine.setTransactionCode(null);
            fineInvoiceRepository.save(fine);
        } else {
            updateFinePayment(fine, paymentMethod, transactionCode, librarianId);
        }
    }

    @Override
    public void processLost(Integer ticketDetailId, boolean payNow, String paymentMethod, String transactionCode,
            String librarianId, String notes) {
        List<FineInvoice> fines = violationService.createLostBookFine(ticketDetailId, notes);
        for (FineInvoice fine : fines) {
            if (payNow) {
                fine.setPaidStatus("Paid");
                fine.setRemainingAmount(BigDecimal.ZERO);
                fine.setPaidAt(LocalDateTime.now());
                updateFinePayment(fine, paymentMethod, transactionCode, librarianId);
            } else {
                fine.setPaidStatus("Unpaid");
                fineInvoiceRepository.save(fine);
            }
        }
    }

    @Override
    public void processDamaged(Integer ticketDetailId, boolean payNow, String paymentMethod, String transactionCode,
            String librarianId, String notes) {
        List<FineInvoice> fines = violationService.createDamagedBookFine(ticketDetailId, notes);
        for (FineInvoice fine : fines) {
            if (payNow) {
                fine.setPaidStatus("Paid");
                fine.setRemainingAmount(BigDecimal.ZERO);
                fine.setPaidAt(LocalDateTime.now());
                updateFinePayment(fine, paymentMethod, transactionCode, librarianId);
            } else {
                fine.setPaidStatus("Unpaid");
                fineInvoiceRepository.save(fine);
            }
        }
    }

    private void updateFinePayment(FineInvoice fine, String paymentMethod, String transactionCode, String librarianId) {
        String targetLibrarianId = librarianId;
        if ("SYSTEM_AUTO".equals(targetLibrarianId) && fine.getTicketDetail() != null) {
            String activeLibrarian = getActiveLibrarian(fine.getTicketDetail().getTicketDetailId());
            if (activeLibrarian != null) {
                targetLibrarianId = activeLibrarian;
            }
        }

        User librarian = null;
        if (targetLibrarianId != null && !targetLibrarianId.isBlank()) {
            librarian = userRepository.findById(targetLibrarianId).orElse(null);
        }
        if (librarian == null) {
            librarian = userRepository.findAll().stream().findFirst().orElse(null);
        }

        fine.setPaymentMethod(paymentMethod != null ? paymentMethod : "Cash");
        if ("QRCode".equalsIgnoreCase(paymentMethod)) {
            fine.setTransactionCode(transactionCode != null && !transactionCode.isBlank() ? transactionCode
                    : "QR-" + System.currentTimeMillis());
        }
        if (librarian != null) {
            fine.setProcessedBy(librarian);
        }
        fineInvoiceRepository.save(fine);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isBookReturned(Integer ticketDetailId) {
        return borrowTicketDetailRepository.findById(ticketDetailId)
                .map(detail -> "Returned".equalsIgnoreCase(detail.getStatus())
                        || "Lost".equalsIgnoreCase(detail.getStatus())
                        || "Damaged".equalsIgnoreCase(detail.getStatus()))
                .orElse(false);
    }

    @Override
    public void registerActiveLibrarian(Integer ticketDetailId, String librarianId) {
        if (ticketDetailId != null && librarianId != null) {
            activeQrLibrarians.put(ticketDetailId, librarianId);
        }
    }

    @Override
    public String getActiveLibrarian(Integer ticketDetailId) {
        if (ticketDetailId == null) {
            return null;
        }
        return activeQrLibrarians.remove(ticketDetailId);
    }
}
