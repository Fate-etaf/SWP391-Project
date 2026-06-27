package com.swp5.library_management.service;

import com.swp5.library_management.entity.MaterialRequest;
import com.swp5.library_management.entity.Notification;
import com.swp5.library_management.entity.User;
import com.swp5.library_management.repository.MaterialRequestRepository;
import com.swp5.library_management.repository.NotificationRepository;
import com.swp5.library_management.repository.UserRepository;

import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Transactional(readOnly = true)
public class MaterialRequestServiceImpl implements MaterialRequestService {

    private static final Logger log = LoggerFactory.getLogger(MaterialRequestServiceImpl.class);

    private final UserRepository userRepository;
    private final MaterialRequestRepository materialRequestRepository;
    private final NotificationRepository notificationRepository;
    private final EmailService emailService;

    public MaterialRequestServiceImpl(UserRepository userRepository,
                                     MaterialRequestRepository materialRequestRepository,
                                     NotificationRepository notificationRepository,
                                     EmailService emailService) {
        this.userRepository = userRepository;
        this.materialRequestRepository = materialRequestRepository;
        this.notificationRepository = notificationRepository;
        this.emailService = emailService;
    }

    @Override
    @Transactional
    public MaterialRequest createMaterialRequest(String patronId, MaterialRequest request) {
        User patron = userRepository.findById(patronId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy tài khoản: " + patronId));

        // 1. Save request to database
        request.setPatron(patron);
        request.setStatus("Pending");
        request.setCreatedAt(LocalDateTime.now());
        MaterialRequest savedRequest = materialRequestRepository.save(request);

        // 2. Save internal system notification
        String notifContent = String.format(
                "Yêu cầu đề nghị tài liệu mới \"%s\" (Tác giả: %s) đã được ghi nhận. Trạng thái: Chờ duyệt.",
                request.getTitle(),
                request.getAuthor()
        );

        notificationRepository.save(Notification.builder()
                .user(patron)
                .notificationType("MATERIAL_REQUEST_SUBMITTED")
                .title("Đề nghị tài liệu mới thành công")
                .content(notifContent)
                .status("Pending")
                .createdAt(LocalDateTime.now())
                .build());

        // 3. Send email confirmation to the email address specified in the request form
        emailService.sendMaterialRequestConfirmation(
                request.getEmail(),
                patron.getFullName(),
                request.getTitle(),
                request.getAuthor(),
                request.getPriority()
        );

        log.info("[MaterialRequest] Request #{} created for Patron={}. Email confirmation sent to={}",
                savedRequest.getRequestId(), patronId, request.getEmail());

        return savedRequest;
    }

    @Override
    @Transactional
    public MaterialRequest approveRequest(Integer requestId, String librarianId) {
        MaterialRequest request = materialRequestRepository.findById(requestId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy yêu cầu: " + requestId));

        // ── Campus restriction: librarian can only approve requests from their own campus ──
        User librarian = userRepository.findById(librarianId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy tài khoản thủ thư: " + librarianId));

        Integer librarianCampusId = librarian.getCampusId();
        Integer patronCampusId   = request.getPatron().getCampusId();

        if (librarianCampusId == null || !librarianCampusId.equals(patronCampusId)) {
            log.warn("[MaterialRequest] Campus mismatch — Librarian={} (campus={}) attempted to approve Request #{} from Patron={} (campus={}).",
                    librarianId, librarianCampusId, requestId, request.getPatron().getUserId(), patronCampusId);
            throw new IllegalStateException(
                    "Bạn không thể duyệt yêu cầu từ bạn đọc thuộc cơ sở khác. " +
                    "Yêu cầu này đến từ cơ sở #" + patronCampusId + ", trong khi bạn thuộc cơ sở #" + librarianCampusId + ".");
        }

        request.setStatus("Approved");
        request.setReviewedBy(librarianId);
        request.setReviewedAt(LocalDateTime.now());

        MaterialRequest savedRequest = materialRequestRepository.save(request);

        User patron = request.getPatron();

        // Notification
        String notifContent = String.format(
                "Yêu cầu đề nghị tài liệu \"%s\" (Tác giả: %s) đã được DUYỆT.",
                request.getTitle(),
                request.getAuthor()
        );

        notificationRepository.save(Notification.builder()
                .user(patron)
                .notificationType("MATERIAL_REQUEST_APPROVED")
                .title("Đề nghị tài liệu được duyệt")
                .content(notifContent)
                .status("Pending")
                .createdAt(LocalDateTime.now())
                .build());

        // Email
        emailService.sendMaterialRequestApproval(
                request.getEmail(),
                patron.getFullName(),
                request.getTitle(),
                request.getAuthor()
        );

        log.info("[MaterialRequest] Request #{} approved by Librarian={}", requestId, librarianId);

        return savedRequest;
    }

    @Override
    @Transactional
    public MaterialRequest rejectRequest(Integer requestId, String librarianId) {
        MaterialRequest request = materialRequestRepository.findById(requestId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy yêu cầu: " + requestId));

        User librarian = userRepository.findById(librarianId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy tài khoản thủ thư: " + librarianId));

        Integer librarianCampusId = librarian.getCampusId();
        Integer patronCampusId   = request.getPatron().getCampusId();

        if (librarianCampusId == null || !librarianCampusId.equals(patronCampusId)) {
            log.warn("[MaterialRequest] Campus mismatch — Librarian={} (campus={}) attempted to reject Request #{} from Patron={} (campus={}).",
                    librarianId, librarianCampusId, requestId, request.getPatron().getUserId(), patronCampusId);
            throw new IllegalStateException(
                    "Bạn không thể từ chối yêu cầu từ bạn đọc thuộc cơ sở khác. " +
                    "Yêu cầu này đến từ cơ sở #" + patronCampusId + ", trong khi bạn thuộc cơ sở #" + librarianCampusId + ".");
        }

        request.setStatus("Rejected");
        request.setReviewedBy(librarianId);
        request.setReviewedAt(LocalDateTime.now());

        MaterialRequest savedRequest = materialRequestRepository.save(request);

        User patron = request.getPatron();

        // Notification
        String notifContent = String.format(
                "Yêu cầu đề nghị tài liệu \"%s\" (Tác giả: %s) đã bị TỪ CHỐI.",
                request.getTitle(),
                request.getAuthor()
        );

        notificationRepository.save(Notification.builder()
                .user(patron)
                .notificationType("MATERIAL_REQUEST_REJECTED")
                .title("Đề nghị tài liệu bị từ chối")
                .content(notifContent)
                .status("Pending")
                .createdAt(LocalDateTime.now())
                .build());

        // Email
        emailService.sendMaterialRequestRejection(
                request.getEmail(),
                patron.getFullName(),
                request.getTitle(),
                request.getAuthor()
        );

        log.info("[MaterialRequest] Request #{} rejected by Librarian={}", requestId, librarianId);

        return savedRequest;
    }
}
