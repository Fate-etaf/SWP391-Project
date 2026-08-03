package com.swp5.library_management.service;

import com.swp5.library_management.entity.BorrowTicketDetail;
import com.swp5.library_management.entity.FineInvoice;
import com.swp5.library_management.entity.User;
import com.swp5.library_management.repository.BorrowTicketDetailRepository;
import com.swp5.library_management.repository.FineInvoiceRepository;
import com.swp5.library_management.repository.UserRepository;
import com.swp5.library_management.entity.Notification;
import com.swp5.library_management.repository.NotificationRepository;
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
    private final EmailService emailService;
    private final NotificationRepository notificationRepository;

    @Override
    @Transactional(readOnly = true)
    public List<BorrowTicketDetail> getCurrentlyBorrowing() {
        return borrowTicketDetailRepository.findCurrentlyBorrowing();
    }

    @Override
    @Transactional(readOnly = true)
    public List<BorrowTicketDetail> searchCurrentlyBorrowing(String title, String borrowerId, String librarianId) {
        String t = (title == null || title.isBlank()) ? null : title.trim();
        String b = (borrowerId == null || borrowerId.isBlank()) ? null : borrowerId.trim();

        Integer campusId = null;
        if (librarianId != null && !librarianId.isBlank()) {
            User librarian = userRepository.findById(librarianId).orElse(null);
            if (librarian != null) {
                campusId = librarian.getCampusId();
            }
        }

        return borrowTicketDetailRepository.searchCurrentlyBorrowing(t, b, campusId);
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
        if (bookPrice.compareTo(BigDecimal.ZERO) >= 0) {
            result.put("combinedAmount", fineAmount.add(bookPrice));
        } else {
            result.put("combinedAmount", BigDecimal.valueOf(-1));
        }
        result.put("conditionStatus",
                detail.getBookCopy() != null ? detail.getBookCopy().getConditionStatus() : "Fair");

        return result;
    }

    @Override
    public void processNormalReturn(Integer ticketDetailId, String conditionStatus, String librarianId) {
        violationService.returnBook(ticketDetailId, conditionStatus, librarianId);

        try {
            BorrowTicketDetail detail = borrowTicketDetailRepository.findById(ticketDetailId).orElse(null);
            if (detail != null && detail.getBorrowTicket() != null && detail.getBorrowTicket().getPatron() != null) {
                User patron = detail.getBorrowTicket().getPatron();
                String toEmail = patron.getEmail();
                String patronName = patron.getFullName();
                String bookTitle = (detail.getBookCopy() != null && detail.getBookCopy().getBook() != null)
                        ? detail.getBookCopy().getBook().getTitle()
                        : "N/A";
                String copyId = detail.getBookCopy() != null ? detail.getBookCopy().getCopyId() : "N/A";
                String returnDateStr = java.time.LocalDateTime.now()
                        .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));

                emailService.sendReturnConfirmation(toEmail, patronName, bookTitle, copyId,
                        returnDateStr, "0 VND", "Không có", "Đúng hạn");

                saveReturnNotification(patron, bookTitle, "Đúng hạn", "0 VND");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public void processOverdueReturn(Integer ticketDetailId, String paymentMethod, String transactionCode,
            String librarianId, String conditionStatus) {
        FineInvoice fine = violationService.createOverdueFine(ticketDetailId, conditionStatus, librarianId);
        if ("PayLater".equalsIgnoreCase(paymentMethod)) {
            fine.setPaidStatus("Unpaid");
            fine.setRemainingAmount(fine.getFineAmount());
            fine.setPaidAt(null);
            fine.setPaymentMethod(null);
            fine.setTransactionCode(null);
            fineInvoiceRepository.save(fine);
        } else {
            fine.setPaidStatus("Paid");
            fine.setRemainingAmount(BigDecimal.ZERO);
            fine.setPaidAt(LocalDateTime.now());
            updateFinePayment(fine, paymentMethod, transactionCode, librarianId);
        }

        try {
            BorrowTicketDetail detail = borrowTicketDetailRepository.findById(ticketDetailId).orElse(null);
            if (detail != null && detail.getBorrowTicket() != null && detail.getBorrowTicket().getPatron() != null) {
                User patron = detail.getBorrowTicket().getPatron();
                String toEmail = patron.getEmail();
                String patronName = patron.getFullName();
                String bookTitle = (detail.getBookCopy() != null && detail.getBookCopy().getBook() != null)
                        ? detail.getBookCopy().getBook().getTitle()
                        : "N/A";
                String copyId = detail.getBookCopy() != null ? detail.getBookCopy().getCopyId() : "N/A";
                String returnDateStr = java.time.LocalDateTime.now()
                        .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));

                String fineAmountStr = String.format("%,.0f VND",
                        fine.getFineAmount() != null ? fine.getFineAmount() : BigDecimal.ZERO);
                String payMethodDisplay = "PayLater".equalsIgnoreCase(paymentMethod) ? "Ghi nợ (Thanh toán sau)"
                        : ("QRCode".equalsIgnoreCase(paymentMethod) ? "QRCode - Chuyển khoản" : "Tiền mặt");

                emailService.sendReturnConfirmation(toEmail, patronName, bookTitle, copyId,
                        returnDateStr, fineAmountStr, payMethodDisplay, "Quá hạn");

                saveReturnNotification(patron, bookTitle, "Quá hạn", fineAmountStr);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void processLost(Integer ticketDetailId, String paymentMethod, String transactionCode,
            String librarianId, String notes, java.math.BigDecimal manualFineAmount) {
        List<FineInvoice> fines = violationService.createLostBookFine(ticketDetailId, notes, librarianId, manualFineAmount);
        BigDecimal totalFine = BigDecimal.ZERO;
        BigDecimal overdueFineAmount = BigDecimal.ZERO;
        BigDecimal lostFineAmount = BigDecimal.ZERO;

        for (FineInvoice fine : fines) {
            fine.setPaidStatus("Paid");
            fine.setRemainingAmount(BigDecimal.ZERO);
            fine.setPaidAt(LocalDateTime.now());
            updateFinePayment(fine, paymentMethod, transactionCode, librarianId);

            if (fine.getFineAmount() != null) {
                totalFine = totalFine.add(fine.getFineAmount());
                if ("OVERDUE".equalsIgnoreCase(fine.getViolationType())) {
                    overdueFineAmount = overdueFineAmount.add(fine.getFineAmount());
                } else if ("LOST".equalsIgnoreCase(fine.getViolationType())) {
                    lostFineAmount = lostFineAmount.add(fine.getFineAmount());
                }
            }
        }

        try {
            BorrowTicketDetail detail = borrowTicketDetailRepository.findById(ticketDetailId).orElse(null);
            if (detail != null && detail.getBorrowTicket() != null && detail.getBorrowTicket().getPatron() != null) {
                User patron = detail.getBorrowTicket().getPatron();
                String toEmail = patron.getEmail();
                String patronName = patron.getFullName();
                String bookTitle = (detail.getBookCopy() != null && detail.getBookCopy().getBook() != null)
                        ? detail.getBookCopy().getBook().getTitle()
                        : "N/A";
                String copyId = detail.getBookCopy() != null ? detail.getBookCopy().getCopyId() : "N/A";
                String returnDateStr = java.time.LocalDateTime.now()
                        .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));

                String fineAmountStr;
                if (overdueFineAmount.compareTo(BigDecimal.ZERO) > 0 && lostFineAmount.compareTo(BigDecimal.ZERO) > 0) {
                    fineAmountStr = String.format(
                            "%,.0f VND (bao gồm: Phạt mất sách %,.0f VND + Phạt quá hạn %,.0f VND)",
                            totalFine, lostFineAmount, overdueFineAmount);
                } else {
                    fineAmountStr = String.format("%,.0f VND", totalFine);
                }

                String payMethodDisplay = "QRCode".equalsIgnoreCase(paymentMethod) ? "QRCode - Chuyển khoản"
                        : "Tiền mặt";

                emailService.sendReturnConfirmation(toEmail, patronName, bookTitle, copyId,
                        returnDateStr, fineAmountStr, payMethodDisplay, "Báo mất sách");

                saveReturnNotification(patron, bookTitle, "Báo mất sách", fineAmountStr);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void processDamaged(Integer ticketDetailId, String paymentMethod, String transactionCode,
            String librarianId, String notes, java.math.BigDecimal manualFineAmount) {
        List<FineInvoice> fines = violationService.createDamagedBookFine(ticketDetailId, notes, librarianId, manualFineAmount);
        BigDecimal totalFine = BigDecimal.ZERO;
        BigDecimal overdueFineAmount = BigDecimal.ZERO;
        BigDecimal damagedFineAmount = BigDecimal.ZERO;

        for (FineInvoice fine : fines) {
            fine.setPaidStatus("Paid");
            fine.setRemainingAmount(BigDecimal.ZERO);
            fine.setPaidAt(LocalDateTime.now());
            updateFinePayment(fine, paymentMethod, transactionCode, librarianId);

            if (fine.getFineAmount() != null) {
                totalFine = totalFine.add(fine.getFineAmount());
                if ("OVERDUE".equalsIgnoreCase(fine.getViolationType())) {
                    overdueFineAmount = overdueFineAmount.add(fine.getFineAmount());
                } else if ("DAMAGED".equalsIgnoreCase(fine.getViolationType())) {
                    damagedFineAmount = damagedFineAmount.add(fine.getFineAmount());
                }
            }
        }

        try {
            BorrowTicketDetail detail = borrowTicketDetailRepository.findById(ticketDetailId).orElse(null);
            if (detail != null && detail.getBorrowTicket() != null && detail.getBorrowTicket().getPatron() != null) {
                User patron = detail.getBorrowTicket().getPatron();
                String toEmail = patron.getEmail();
                String patronName = patron.getFullName();
                String bookTitle = (detail.getBookCopy() != null && detail.getBookCopy().getBook() != null)
                        ? detail.getBookCopy().getBook().getTitle()
                        : "N/A";
                String copyId = detail.getBookCopy() != null ? detail.getBookCopy().getCopyId() : "N/A";
                String returnDateStr = java.time.LocalDateTime.now()
                        .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));

                String fineAmountStr;
                if (overdueFineAmount.compareTo(BigDecimal.ZERO) > 0
                        && damagedFineAmount.compareTo(BigDecimal.ZERO) > 0) {
                    fineAmountStr = String.format(
                            "%,.0f VND (bao gồm: Phạt hỏng sách %,.0f VND + Phạt quá hạn %,.0f VND)",
                            totalFine, damagedFineAmount, overdueFineAmount);
                } else {
                    fineAmountStr = String.format("%,.0f VND", totalFine);
                }

                String payMethodDisplay = "QRCode".equalsIgnoreCase(paymentMethod) ? "QRCode - Chuyển khoản"
                        : "Tiền mặt";

                emailService.sendReturnConfirmation(toEmail, patronName, bookTitle, copyId,
                        returnDateStr, fineAmountStr, payMethodDisplay, "Báo hỏng sách");

                saveReturnNotification(patron, bookTitle, "Báo hỏng sách", fineAmountStr);
            }
        } catch (Exception e) {
            e.printStackTrace();
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
            librarian = userRepository.findAnyLibrarian().stream().findFirst().orElse(null);
            if (librarian == null) {
                librarian = userRepository.findAll().stream().findFirst().orElse(null);
            }
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

    private void saveReturnNotification(User patron, String bookTitle, String status, String fineAmount) {
        String msg = String.format("Ghi nhận trả sách \"%s\" thành công. Trạng thái: %s. Phí phạt phát sinh: %s.",
                bookTitle, status, fineAmount);

        Notification notification = Notification.builder()
                .user(patron)
                .notificationType("BOOK_RETURNED")
                .title("Trả sách thành công")
                .content(msg)
                .status("Sent")
                .sentAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .read(false)
                .build();
        notificationRepository.save(notification);
    }
}
