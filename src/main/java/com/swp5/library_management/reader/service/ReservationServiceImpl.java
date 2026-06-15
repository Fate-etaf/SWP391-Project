package com.swp5.library_management.reader.service;

import com.swp5.library_management.reader.dto.ReservationResultDTO;
import com.swp5.library_management.entity.*;
import com.swp5.library_management.librarian.service.EmailService;
import com.swp5.library_management.repository.*;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * Triển khai đầy đủ Use Case UCR06 – Reserve Book Online.
 *
 * LUỒNG CHÍNH (Normal Flow):
 *   Bước 2 → Kiểm tra trạng thái tài khoản & cờ khóa     → Exc 1
 *   Bước 3 → Kiểm tra giới hạn số đơn đặt giữ chỗ        → Exc 2
 *   Bước 4 → Tìm bản sách Available tại campus             → Exc 3 (waitlist)
 *   Bước 5 → Lock bản sách (@Transactional pessimistic)    → Exc 4 (race → waitlist)
 *   Bước 6 → Cập nhật CopyStatus → "Reserved"
 *   Bước 7 → Tạo Reservation (status=Holding, 24h expiry)
 *   Bước 8 → Ghi Notification + gửi Email xác nhận
 *
 * ALT 1: cancelReservation → Cancelled + BookCopy → Available
 * EXC 3/4: joinWaitlist → Ghi Waitlist + gửi Email
 */
@Service
@Transactional(readOnly = true)
public class ReservationServiceImpl implements ReservationService {

    private static final Logger log = LoggerFactory.getLogger(ReservationServiceImpl.class);

    private final UserRepository        userRepository;
    private final BookRepository        bookRepository;
    private final BookCopyRepository    bookCopyRepository;
    private final CampusRepository      campusRepository;
    private final ReservationRepository reservationRepository;
    private final WaitlistRepository    waitlistRepository;
    private final NotificationRepository notificationRepository;
    private final EmailService          emailService;
    private final com.swp5.library_management.librarian.service.SystemConfigService systemConfigService;
    private final TransferRequestRepository transferRequestRepository;
    private final TransferDetailRepository  transferDetailRepository;

    public ReservationServiceImpl(UserRepository userRepository,
                                  BookRepository bookRepository,
                                  BookCopyRepository bookCopyRepository,
                                  CampusRepository campusRepository,
                                  ReservationRepository reservationRepository,
                                  WaitlistRepository waitlistRepository,
                                  NotificationRepository notificationRepository,
                                  EmailService emailService,
                                  com.swp5.library_management.librarian.service.SystemConfigService systemConfigService,
                                  TransferRequestRepository transferRequestRepository,
                                  TransferDetailRepository transferDetailRepository) {
        this.userRepository        = userRepository;
        this.bookRepository        = bookRepository;
        this.bookCopyRepository    = bookCopyRepository;
        this.campusRepository      = campusRepository;
        this.reservationRepository = reservationRepository;
        this.waitlistRepository    = waitlistRepository;
        this.notificationRepository = notificationRepository;
        this.emailService          = emailService;
        this.systemConfigService   = systemConfigService;
        this.transferRequestRepository = transferRequestRepository;
        this.transferDetailRepository = transferDetailRepository;
    }

    // =========================================================================
    // NORMAL FLOW: reserveBook
    // =========================================================================

    @Override
    @Transactional
    public ReservationResultDTO reserveBook(String patronId, Integer bookId, Integer pickupCampusId) {

        // ── Bước 2: Kiểm tra tài khoản bạn đọc ─────────────────────────────
        User patron = userRepository.findById(patronId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy tài khoản: " + patronId));

        if (!"Active".equals(patron.getStatus())) {
            // Exc 1a: Tài khoản không active
            return ReservationResultDTO.builder()
                    .success(false)
                    .resultType("ERROR")
                    .message("Giao dịch thất bại! Tài khoản của bạn đang ở trạng thái " +
                             patron.getStatus() + ". Vui lòng liên hệ thủ thư để được hỗ trợ.")
                    .build();
        }

        if (Boolean.TRUE.equals(patron.getBorrowingLocked())) {
            // Exc 1b: Tài khoản bị khóa do nợ phạt
            return ReservationResultDTO.builder()
                    .success(false)
                    .resultType("ERROR")
                    .message("Giao dịch thất bại! Tài khoản của bạn đang bị khóa do có khoản phạt " +
                             "chưa thanh toán. Vui lòng hoàn tất nộp phạt trước khi đặt sách.")
                    .build();
        }

        // ── Bước 3: Kiểm tra giới hạn đơn đặt giữ chỗ ──────────────────────
        int maxActiveReservations = systemConfigService.getIntConfig("MAX_BOOKS_STUDENT", 3);
        long activeCount = reservationRepository.countByPatronUserIdAndStatus(patronId, "Holding");
        if (activeCount >= maxActiveReservations) {
            // Exc 2: Vượt giới hạn
            return ReservationResultDTO.builder()
                    .success(false)
                    .resultType("ERROR")
                    .message("Thao tác bị từ chối! Bạn đã có " + activeCount + "/" + maxActiveReservations +
                             " đơn đặt giữ chỗ đang hoạt động. Vui lòng hủy bớt đơn cũ trước khi tiếp tục.")
                    .build();
        }

        // ── Lấy thông tin sách và campus ────────────────────────────────────
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy sách: " + bookId));

        Campus campus = campusRepository.findById(pickupCampusId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy cơ sở: " + pickupCampusId));

        // ── Bước 4: Tìm bản sách Available tại campus được chọn ─────────────
        Optional<BookCopy> availableCopyOpt = bookCopyRepository
                .findFirstByBookBookIdAndCampusCampusIdAndCopyStatus(bookId, pickupCampusId, "Available");

        if (availableCopyOpt.isEmpty()) {
            // Exc 3: Hết sách tại campus → chuyển sang trang waitlist
            return ReservationResultDTO.builder()
                    .success(false)
                    .resultType("NO_COPY")
                    .message("Rất tiếc, đầu sách này hiện đã hết bản sẵn sàng tại cơ sở bạn chọn. " +
                             "Bạn có muốn đăng ký xếp hàng vào danh sách chờ không?")
                    .build();
        }

        // ── Bước 5: Lock bản sách (Pessimistic locking qua @Transactional) ──
        BookCopy copy = availableCopyOpt.get();

        // Kiểm tra lại trạng thái sau khi lấy ra (Exc 4: race condition)
        if (!"Available".equals(copy.getCopyStatus())) {
            log.warn("[UCR06 Exc 4] Race condition: CopyID={} vừa bị đặt bởi user khác.", copy.getCopyId());
            return ReservationResultDTO.builder()
                    .success(false)
                    .resultType("NO_COPY")
                    .message("Bản sách vừa được đặt bởi bạn đọc khác. " +
                             "Bạn có muốn đăng ký xếp hàng vào danh sách chờ không?")
                    .build();
        }

        // ── Bước 6: Cập nhật trạng thái bản sách → Reserved ────────────────
        copy.setCopyStatus("Reserved");
        bookCopyRepository.save(copy);

        // ── Branch 3: Rẽ nhánh mượn liên cơ sở (Khác Campus) ────────────────
        if (!patron.getCampusId().equals(pickupCampusId)) {
            Campus userCampus = campusRepository.findById(patron.getCampusId())
                    .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy cơ sở người dùng: " + patron.getCampusId()));

            TransferRequest transferRequest = TransferRequest.builder()
                    .requestedBy(patron)
                    .fromCampus(campus)
                    .toCampus(userCampus)
                    .requestedAt(LocalDateTime.now())
                    .status("Pending")
                    .build();
            transferRequest = transferRequestRepository.saveAndFlush(transferRequest);

            TransferDetail detail = TransferDetail.builder()
                    .id(new TransferDetailId(transferRequest.getTransferId(), copy.getCopyId()))
                    .transferRequest(transferRequest)
                    .copy(copy)
                    .build();
            transferDetailRepository.save(detail);

            notificationRepository.save(Notification.builder()
                    .user(patron)
                    .notificationType("INTER_CAMPUS_REQUESTED")
                    .title("Yêu cầu mượn liên cơ sở")
                    .content("Bạn đã yêu cầu mượn cuốn \"" + book.getTitle() + "\" từ " + campus.getCampusName() + ". Yêu cầu đang chờ thủ thư xử lý.")
                    .status("Pending")
                    .createdAt(LocalDateTime.now())
                    .build());

            log.info("[UCR15] Inter-campus request created: Patron={}, Book={}, Copy={}, From={}, To={}", patronId, bookId, copy.getCopyId(), pickupCampusId, patron.getCampusId());

            return ReservationResultDTO.builder()
                    .success(true)
                    .resultType("RESERVED")
                    .message("Sách hiện không có tại cơ sở của bạn. Yêu cầu mượn liên cơ sở đã được gửi đến thủ thư và đang chờ duyệt!")
                    .build();
        }

        // ── Bước 7: Tạo đơn Reservation (Branch 1) ──────────────────────────

        // ── Bước 7: Tạo đơn Reservation ─────────────────────────────────────
        int holdHours = systemConfigService.getIntConfig("RESERVATION_EXPIRE_HR", 72);
        LocalDateTime now        = LocalDateTime.now();
        LocalDateTime expiration = now.plusHours(holdHours);

        Reservation reservation = Reservation.builder()
                .patron(patron)
                .book(book)
                .copy(copy)
                .pickupCampus(campus)
                .reservedAt(now)
                .expirationDate(expiration)
                .status("Holding")
                .build();
        reservationRepository.save(reservation);

        // ── Bước 8a: Ghi Notification nội bộ ────────────────────────────────
        String notifContent = String.format(
                "Đơn đặt giữ chỗ #%d cho sách \"%s\" tại %s đã được xác nhận. " +
                "Vui lòng đến nhận sách trước %s.",
                reservation.getReservationId(),
                book.getTitle(),
                campus.getCampusName(),
                expiration.format(DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy"))
        );

        notificationRepository.save(Notification.builder()
                .user(patron)
                .notificationType("RESERVATION_CONFIRMED")
                .title("Đặt giữ chỗ sách thành công")
                .content(notifContent)
                .status("Pending")
                .createdAt(now)
                .build());

        // ── Bước 8b: Gửi Email xác nhận ─────────────────────────────────────
        String expiryInfo = "trước " + expiration.format(DateTimeFormatter.ofPattern("HH:mm ngày dd/MM/yyyy"));
        emailService.sendReservationConfirmation(
                patron.getEmail(),
                patron.getFullName(),
                book.getTitle(),
                campus.getCampusName(),
                expiryInfo
        );

        log.info("[UCR06] Reservation #{} created: Patron={}, Book={}, Copy={}, Campus={}, Expiry={}",
                reservation.getReservationId(), patronId, bookId, copy.getCopyId(), pickupCampusId, expiration);

        return ReservationResultDTO.builder()
                .success(true)
                .resultType("RESERVED")
                .message("Đặt giữ chỗ sách thành công! Vui lòng đến nhận sách " + expiryInfo + ".")
                .reservationId(reservation.getReservationId())
                .expirationDate(expiration)
                .build();
    }

    // =========================================================================
    // ALT 1: cancelReservation
    // =========================================================================

    @Override
    @Transactional
    public void cancelReservation(String patronId, Integer reservationId) {

        // Alt 1 – Bước 2: Lấy đơn và kiểm tra quyền sở hữu
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy đơn đặt chỗ #" + reservationId));

        if (!reservation.getPatron().getUserId().equals(patronId)) {
            throw new SecurityException("Bạn không có quyền hủy đơn đặt chỗ này.");
        }

        if (!"Holding".equals(reservation.getStatus())) {
            throw new IllegalStateException("Chỉ có thể hủy đơn đang ở trạng thái Holding.");
        }

        // Alt 1 – Bước 5a: Cập nhật trạng thái đơn → Cancelled
        reservation.setStatus("Cancelled");
        reservationRepository.save(reservation);

        // Alt 1 – Bước 5b: Trả bản sách về trạng thái Available
        BookCopy copy = reservation.getCopy();
        if (copy != null) {
            copy.setCopyStatus("Available");
            bookCopyRepository.save(copy);
        }

        // Alt 1 – Bước 6a: Ghi Notification hủy đơn
        LocalDateTime now = LocalDateTime.now();
        notificationRepository.save(Notification.builder()
                .user(reservation.getPatron())
                .notificationType("RESERVATION_CANCELLED")
                .title("Hủy đặt giữ chỗ thành công")
                .content(String.format("Đơn đặt giữ chỗ #%d cho sách \"%s\" đã được hủy thành công.",
                        reservationId, reservation.getBook().getTitle()))
                .status("Pending")
                .createdAt(now)
                .build());

        // Alt 1 – Bước 6b: Gửi Email xác nhận hủy
        emailService.sendReservationCancellation(
                reservation.getPatron().getEmail(),
                reservation.getPatron().getFullName(),
                reservation.getBook().getTitle()
        );

        log.info("[UCR06 Alt1] Reservation #{} cancelled by Patron={}", reservationId, patronId);
    }

    // =========================================================================
    // Lấy danh sách đặt chỗ
    // =========================================================================

    @Override
    public List<Reservation> getMyReservations(String patronId) {
        return reservationRepository.findByPatronUserIdOrderByReservedAtDesc(patronId);
    }

    @Override
    public List<Waitlist> getMyWaitlists(String patronId) {
        return waitlistRepository.findByPatronUserIdOrderByRequestedAtDesc(patronId);
    }

    // =========================================================================
    // EXC 3/4: joinWaitlist
    // =========================================================================

    @Override
    @Transactional
    public ReservationResultDTO joinWaitlist(String patronId, Integer bookId, Integer campusId) {

        User patron = userRepository.findById(patronId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy tài khoản: " + patronId));
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy sách: " + bookId));
        Campus campus = campusRepository.findById(campusId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy cơ sở: " + campusId));

        // Kiểm tra đã có trong hàng chờ chưa
        boolean alreadyWaiting = waitlistRepository.existsByBookBookIdAndPatronUserIdAndStatusIn(
                bookId, patronId, List.of("Waiting", "Notified"));

        if (alreadyWaiting) {
            return ReservationResultDTO.builder()
                    .success(false)
                    .resultType("ERROR")
                    .message("Bạn đã có trong danh sách chờ cho cuốn sách này tại cơ sở đã chọn.")
                    .build();
        }

        // Ghi vào Waitlist
        Waitlist waitlist = Waitlist.builder()
                .book(book)
                .patron(patron)
                .campus(campus)
                .requestedAt(LocalDateTime.now())
                .status("Waiting")
                .build();
        waitlistRepository.save(waitlist);

        // Tính số thứ tự trong hàng chờ
        long position = waitlistRepository.countByBookBookIdAndCampusCampusIdAndStatusIn(
                bookId, campusId, List.of("Waiting", "Notified"));

        // Ghi Notification
        notificationRepository.save(Notification.builder()
                .user(patron)
                .notificationType("WAITLIST_JOINED")
                .title("Đăng ký hàng đợi thành công")
                .content(String.format("Bạn đã đăng ký xếp hàng chờ sách \"%s\" tại %s. " +
                        "Số thứ tự của bạn: #%d.", book.getTitle(), campus.getCampusName(), position))
                .status("Pending")
                .createdAt(LocalDateTime.now())
                .build());

        // Gửi Email xác nhận waitlist
        emailService.sendWaitlistConfirmation(
                patron.getEmail(),
                patron.getFullName(),
                book.getTitle(),
                position
        );

        log.info("[UCR06 Exc3] Waitlist #{} created: Patron={}, Book={}, Campus={}, Position={}",
                waitlist.getWaitlistId(), patronId, bookId, campusId, position);

        return ReservationResultDTO.builder()
                .success(true)
                .resultType("WAITLISTED")
                .message("Đăng ký xếp hàng chờ thành công! Số thứ tự của bạn: #" + position + ". " +
                         "Hệ thống sẽ thông báo ngay khi có sách.")
                .waitlistId(waitlist.getWaitlistId())
                .waitlistPosition(position)
                .build();
    }

    // =========================================================================
    // ALT 2: cancelWaitlist
    // =========================================================================

    @Override
    @Transactional
    public void cancelWaitlist(String patronId, Integer waitlistId) {
        Waitlist waitlist = waitlistRepository.findById(waitlistId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy đăng ký xếp hàng chờ #" + waitlistId));

        if (!waitlist.getPatron().getUserId().equals(patronId)) {
            throw new SecurityException("Bạn không có quyền hủy đăng ký xếp hàng chờ này.");
        }

        if (!List.of("Waiting", "Notified").contains(waitlist.getStatus())) {
            throw new IllegalStateException("Chỉ có thể hủy đăng ký xếp hàng chờ đang ở trạng thái Waiting hoặc Notified.");
        }

        // Alt 2 – Bước 5a: Cập nhật trạng thái hàng chờ thành Cancelled
        waitlist.setStatus("Cancelled");
        waitlistRepository.save(waitlist);

        // Alt 2 – Bước 5b: Ghi Notification
        LocalDateTime now = LocalDateTime.now();
        notificationRepository.save(Notification.builder()
                .user(waitlist.getPatron())
                .notificationType("WAITLIST_CANCELLED")
                .title("Đã rút tên khỏi hàng đợi thành công")
                .content(String.format("Bạn đã rút tên khỏi hàng đợi chờ sách \"%s\" thành công.",
                        waitlist.getBook().getTitle()))
                .status("Pending")
                .createdAt(now)
                .build());

        // Alt 2 – Bước 5c: Gửi email hủy xếp hàng chờ
        emailService.sendWaitlistCancellation(
                waitlist.getPatron().getEmail(),
                waitlist.getPatron().getFullName(),
                waitlist.getBook().getTitle()
        );

        log.info("[UCR06 Alt2] Waitlist #{} cancelled by Patron={}", waitlistId, patronId);
    }
}
