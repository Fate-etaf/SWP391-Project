package com.swp5.library_management.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.swp5.library_management.entity.Book;
import com.swp5.library_management.entity.BookCopy;
import com.swp5.library_management.entity.BorrowTicketDetail;
import com.swp5.library_management.entity.FineInvoice;
import com.swp5.library_management.entity.User;
import com.swp5.library_management.repository.AcquisitionOrderDetailRepository;
import com.swp5.library_management.repository.BookCopyRepository;
import com.swp5.library_management.repository.BorrowTicketDetailRepository;
import com.swp5.library_management.repository.FineInvoiceRepository;
import com.swp5.library_management.repository.UserRepository;
import com.swp5.library_management.entity.Notification;
import com.swp5.library_management.repository.NotificationRepository;
import com.swp5.library_management.repository.CampusRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class ViolationServiceImpl implements ViolationService {

    private static final BigDecimal OVERDUE_DAILY_FINE = BigDecimal.valueOf(5000);

    private final BorrowTicketDetailRepository borrowTicketDetailRepository;
    private final FineInvoiceRepository fineInvoiceRepository;
    private final BookCopyRepository bookCopyRepository;
    private final AcquisitionOrderDetailRepository acquisitionOrderDetailRepository;
    private final UserRepository userRepository;
    private final ReservationService reservationService;
    private final SystemConfigService systemConfigService;
    private final FineEmailService fineEmailService;
    private final NotificationRepository notificationRepository;
    private final CampusRepository campusRepository;
    
    private void setReturnCampusFromLibrarian(BorrowTicketDetail detail, String librarianId) {
        if (librarianId != null && !librarianId.isBlank()) {
            userRepository.findById(librarianId).ifPresent(librarian -> {
                if (librarian.getCampusId() != null) {
                    campusRepository.findById(librarian.getCampusId()).ifPresent(campus -> {
                        detail.setReturnCampus(campus);
                    });
                }
            });
        }
    }

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
    public void returnBook(Integer borrowTicketDetailId, String conditionStatus, String librarianId) {
        BorrowTicketDetail detail = getDetailOrThrow(borrowTicketDetailId);
        if (detail.getReturnDate() == null) {
            detail.setReturnDate(LocalDateTime.now());
        }
        detail.setStatus("Returned");
        setReturnCampusFromLibrarian(detail, librarianId);
        borrowTicketDetailRepository.save(detail);
        BookCopy copy = detail.getBookCopy();
        if (copy != null) {
            String newCondition = applyConditionStatus(copy.getConditionStatus(), conditionStatus);
            markCopyStatus(copy, "Available", newCondition);
        }
    }

    @Override
    public FineInvoice createOverdueFine(Integer borrowTicketDetailId, String conditionStatus, String librarianId) {
        BorrowTicketDetail detail = getDetailOrThrow(borrowTicketDetailId);
        FineInvoice fine = buildOverdueFine(detail);
        if (detail.getReturnDate() == null) {
            detail.setReturnDate(LocalDateTime.now());
        }
        detail.setStatus("Returned");
        setReturnCampusFromLibrarian(detail, librarianId);
        borrowTicketDetailRepository.save(detail);
        BookCopy copy = detail.getBookCopy();
        if (copy != null) {
            String newCondition = applyConditionStatus(copy.getConditionStatus(), conditionStatus);
            markCopyStatus(copy, "Available", newCondition);
        }
        return fineInvoiceRepository.save(fine);
    }

    @Override
    public List<FineInvoice> createLostBookFine(Integer borrowTicketDetailId, String notes, String librarianId, BigDecimal manualFineAmount) {
        BorrowTicketDetail detail = getDetailOrThrow(borrowTicketDetailId);
        markCopyStatus(detail.getBookCopy(), "Maintenance", "Lost");
        updateTicketDetailStatus(detail, "Lost", librarianId);

        List<FineInvoice> fines = new java.util.ArrayList<>();
        LocalDate dueDate = detail.getDueDate() != null ? detail.getDueDate().toLocalDate() : null;
        if (calculateOverdueDays(dueDate) > 0) {
            fines.add(buildOverdueFine(detail));
        }

        String reason = "Lost book";
        if (notes != null && !notes.isBlank()) {
            reason += " — Ghi chú: " + notes.trim();
        }
        
        BigDecimal fineAmount;
        if (manualFineAmount != null && manualFineAmount.compareTo(BigDecimal.ZERO) >= 0) {
            fineAmount = manualFineAmount;
        } else {
            fineAmount = getLostFineAmount(detail);
        }
        
        fines.add(buildFixedFine(detail, "LOST", fineAmount, reason));
        return fines;
    }

    @Override
    public List<FineInvoice> createDamagedBookFine(Integer borrowTicketDetailId, String notes, String librarianId, BigDecimal manualFineAmount) {
        BorrowTicketDetail detail = getDetailOrThrow(borrowTicketDetailId);
        markCopyStatus(detail.getBookCopy(), "Maintenance", "Damaged");
        updateTicketDetailStatus(detail, "Damaged", librarianId);

        List<FineInvoice> fines = new java.util.ArrayList<>();
        LocalDate dueDate = detail.getDueDate() != null ? detail.getDueDate().toLocalDate() : null;
        if (calculateOverdueDays(dueDate) > 0) {
            fines.add(buildOverdueFine(detail));
        }

        String reason = "Damaged book";
        if (notes != null && !notes.isBlank()) {
            reason += " — Ghi chú: " + notes.trim();
        }

        BigDecimal fineAmount;
        if (manualFineAmount != null && manualFineAmount.compareTo(BigDecimal.ZERO) >= 0) {
            fineAmount = manualFineAmount;
        } else {
            fineAmount = getDamagedFineAmount(detail);
        }

        fines.add(buildFine(detail, fineAmount, "DAMAGED", reason));
        return fines;
    }

    private BorrowTicketDetail getDetailOrThrow(Integer id) {
        return borrowTicketDetailRepository
                .findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Borrow ticket detail not found: " + id));
    }

    private FineInvoice buildOverdueFine(BorrowTicketDetail detail) {
        LocalDate dueDate = detail.getDueDate() != null ? detail.getDueDate().toLocalDate() : null;
        if (dueDate == null) {
            throw new IllegalStateException("Borrow ticket detail has no due date.");
        }
        LocalDate endDate = detail.getReturnDate() != null ? detail.getReturnDate().toLocalDate() : LocalDate.now();
        long overdueDays = Math.max(ChronoUnit.DAYS.between(dueDate, endDate), 0L);
        
        if (overdueDays <= 0) {
            throw new IllegalStateException("Borrow ticket detail is not overdue.");
        }

        BigDecimal finePerDay = BigDecimal.valueOf(systemConfigService.getIntConfig("FINE_PER_DAY", 5000));
        BigDecimal amount = finePerDay.multiply(BigDecimal.valueOf(overdueDays));
        return buildFine(detail, amount, "OVERDUE", "Overdue " + overdueDays + " day(s)");
    }


    private FineInvoice buildFixedFine(BorrowTicketDetail detail, String violationType, BigDecimal amount,
            String reason) {
        return buildFine(detail, amount, violationType, reason);
    }

    private FineInvoice buildFine(BorrowTicketDetail detail, BigDecimal amount, String violationType, String reason) {
        FineInvoice fine = new FineInvoice();
        fine.setTicketDetail(detail);
        User patron = detail.getBorrowTicket() != null ? detail.getBorrowTicket().getPatron() : null;
        fine.setPatron(patron);
        fine.setFineAmount(amount);
        fine.setRemainingAmount(amount);
        fine.setViolationType(violationType);
        fine.setReason(reason);
        fine.setCreatedAt(LocalDateTime.now());
        fine.setPaidStatus("Unpaid");
        
        if (patron != null) {
            patron.setBorrowingLocked(true);
            userRepository.save(patron);
        }
        
        return fineInvoiceRepository.save(fine);
    }

    private BigDecimal getLostFineAmount(BorrowTicketDetail detail) {
        return getBookImportPrice(detail);
    }

    private BigDecimal getDamagedFineAmount(BorrowTicketDetail detail) {
        return getBookImportPrice(detail);
    }

    @Override
    public BigDecimal getBookPriceByTicketDetailId(Integer ticketDetailId) {
        try {
            BorrowTicketDetail detail = getDetailOrThrow(ticketDetailId);
            return getBookImportPrice(detail);
        } catch (IllegalStateException e) {
            return BigDecimal.valueOf(-1);
        }
    }

    private BigDecimal getBookImportPrice(BorrowTicketDetail detail) {
        if (detail.getBookCopy() == null || detail.getBookCopy().getBook() == null) {
            throw new IllegalStateException("Không tìm thấy thông tin sách của bản sao tương ứng.");
        }
        Book book = detail.getBookCopy().getBook();
        return acquisitionOrderDetailRepository
                .findLatestByBook(book)
                .map(d -> d.getUnitPrice())
                .orElseThrow(() -> new IllegalStateException(
                        "Không tìm thấy thông tin đơn nhập hoặc đơn giá hợp lệ của sách: " + book.getTitle()));
    }

    private void markCopyStatus(BookCopy copy, String copyStatus, String conditionStatus) {
        if (copy == null) {
            return;
        }
        copy.setCopyStatus(copyStatus);
        copy.setConditionStatus(conditionStatus);
        bookCopyRepository.save(copy);
    }

    /**
     * Áp dụng tình trạng mới cho sách theo quy tắc CHỈ xuống cấp:
     * New(3) → Good(2) → Fair(1). Không cho phép nâng cấp lên.
     * Nếu newCondition là null/không hợp lệ, giữ nguyên giá trị cũ.
     */
    private String applyConditionStatus(String currentCondition, String newCondition) {
        java.util.Map<String, Integer> rank = java.util.Map.of("New", 3, "Good", 2, "Fair", 1);
        if (newCondition == null || !rank.containsKey(newCondition)) {
            return currentCondition != null ? currentCondition : "Fair";
        }
        int currentRank = rank.getOrDefault(currentCondition, 1);
        int newRank = rank.get(newCondition);
        // Chỉ cho phép khi rank mới <= rank hiện tại (tức xuống cấp hoặc giữ nguyên)
        return (newRank <= currentRank) ? newCondition : currentCondition;
    }

    private void updateTicketDetailStatus(BorrowTicketDetail detail, String status, String librarianId) {
        detail.setStatus(status);
        detail.setReturnDate(LocalDateTime.now());
        setReturnCampusFromLibrarian(detail, librarianId);
        borrowTicketDetailRepository.save(detail);
    }

    @Override
    public BorrowTicketDetail returnByCopyId(String copyId, String librarianId) {
        List<BorrowTicketDetail> actives = borrowTicketDetailRepository.findActiveByCopyId(copyId.trim());
        if (actives.isEmpty()) {
            throw new IllegalArgumentException("Không tìm thấy lượt mượn đang hoạt động cho mã sách: " + copyId);
        }
        BorrowTicketDetail detail = actives.get(0);
        returnBook(detail.getTicketDetailId(), null, librarianId);
        return detail;
    }

    @Override
    @Transactional(readOnly = true)
    public List<FineInvoice> getAllFineInvoices(String patronId, String paidStatus, String librarianId) {
        String pid = (patronId != null && patronId.isBlank()) ? null : patronId;
        String ps = (paidStatus != null && paidStatus.isBlank()) ? null : paidStatus;
        Integer campusId = null;
        if (librarianId != null && !librarianId.isBlank()) {
            User librarian = userRepository.findById(librarianId).orElse(null);
            if (librarian != null) {
                campusId = librarian.getCampusId();
            }
        }
        return fineInvoiceRepository.findAllFiltered(pid, ps, campusId);
    }

    @Override
    public void collectFineCash(Integer fineId, String librarianId) {
        FineInvoice fine = fineInvoiceRepository.findById(fineId)
                .orElseThrow(() -> new IllegalArgumentException("Hóa đơn phạt không tồn tại: " + fineId));

        if ("Paid".equalsIgnoreCase(fine.getPaidStatus())) {
            throw new IllegalStateException("Hóa đơn này đã được thanh toán.");
        }

        User librarian = null;
        if (librarianId != null && !librarianId.isBlank()) {
            librarian = userRepository.findById(librarianId).orElse(null);
        }
        if (librarian == null) {
            librarian = userRepository.findAnyLibrarian().stream().findFirst().orElse(null);
            if (librarian == null) {
                librarian = userRepository.findAll().stream().findFirst().orElse(null);
            }
        }

        fine.setPaidStatus("Paid");
        fine.setRemainingAmount(BigDecimal.ZERO);
        fine.setPaidAt(LocalDateTime.now());
        fine.setPaymentMethod("Cash");
        fine.setProcessedBy(librarian);
        fineInvoiceRepository.save(fine);

        // Tự động mở khóa thẻ mượn nếu sinh viên không còn nợ phạt nào
        User patron = fine.getPatron();
        if (patron != null && Boolean.TRUE.equals(patron.getBorrowingLocked())) {
            List<FineInvoice> remaining = fineInvoiceRepository
                    .findByPatronUserIdAndPaidStatus(patron.getUserId(), "Unpaid");
            if (remaining.isEmpty()) {
                patron.setBorrowingLocked(false);
                userRepository.save(patron);
            }
        }

        // Action-hook: Gửi email thông báo thanh toán thành công
        try {
            fineEmailService.sendFinePaymentConfirmation(fine);
            saveFinePaidNotification(patron, fine);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void collectFineQR(Integer fineId, String librarianId, String transactionCode) {
        FineInvoice fine = fineInvoiceRepository.findById(fineId)
                .orElseThrow(() -> new IllegalArgumentException("Hóa đơn phạt không tồn tại: " + fineId));

        if ("Paid".equalsIgnoreCase(fine.getPaidStatus())) {
            throw new IllegalStateException("Hóa đơn này đã được thanh toán.");
        }

        User librarian = null;
        if (librarianId != null && !librarianId.isBlank()) {
            librarian = userRepository.findById(librarianId).orElse(null);
        }
        if (librarian == null) {
            librarian = userRepository.findAnyLibrarian().stream().findFirst().orElse(null);
            if (librarian == null) {
                librarian = userRepository.findAll().stream().findFirst().orElse(null);
            }
        }

        fine.setPaidStatus("Paid");
        fine.setRemainingAmount(BigDecimal.ZERO);
        fine.setPaidAt(LocalDateTime.now());
        fine.setPaymentMethod("QRCode");
        fine.setTransactionCode(transactionCode != null && !transactionCode.isBlank() ? transactionCode
                : "QR-" + System.currentTimeMillis());
        fine.setProcessedBy(librarian);
        fineInvoiceRepository.save(fine);

        // Tự động mở khóa thẻ mượn nếu sinh viên không còn nợ phạt nào
        User patron = fine.getPatron();
        if (patron != null && Boolean.TRUE.equals(patron.getBorrowingLocked())) {
            List<FineInvoice> remaining = fineInvoiceRepository
                    .findByPatronUserIdAndPaidStatus(patron.getUserId(), "Unpaid");
            if (remaining.isEmpty()) {
                patron.setBorrowingLocked(false);
                userRepository.save(patron);
            }
        }

        // Action-hook: Gửi email thông báo thanh toán thành công
        try {
            fineEmailService.sendFinePaymentConfirmation(fine);
            saveFinePaidNotification(patron, fine);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void saveFinePaidNotification(User patron, FineInvoice fine) {
        if (patron == null || fine == null) return;
        String bookTitle = fine.getTicketDetail() != null && fine.getTicketDetail().getBookCopy() != null 
                           ? fine.getTicketDetail().getBookCopy().getBook().getTitle() : "N/A";
        String amountStr = String.format("%,.0f VND", fine.getFineAmount() != null ? fine.getFineAmount() : BigDecimal.ZERO);
        String methodDisplay = "Cash".equalsIgnoreCase(fine.getPaymentMethod()) ? "Tiền mặt" 
                               : ("QRCode".equalsIgnoreCase(fine.getPaymentMethod()) ? "QRCode" : fine.getPaymentMethod());
        
        String msg = String.format("Thanh toán thành công số tiền phạt %s cho sách \"%s\" qua phương thức %s.",
                amountStr, bookTitle, methodDisplay);
        
        Notification notification = Notification.builder()
                .user(patron)
                .notificationType("FINE_PAID")
                .title("Thanh toán tiền phạt thành công")
                .content(msg)
                .status("Sent")
                .sentAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .read(false)
                .build();
        notificationRepository.save(notification);
    }

    @Override
    @Transactional(readOnly = true)
    public FineInvoice getFineInvoiceById(Integer fineId) {
        return fineInvoiceRepository.findById(fineId).orElse(null);
    }
}
