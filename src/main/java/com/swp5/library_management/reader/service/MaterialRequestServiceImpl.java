package com.swp5.library_management.reader.service;

import com.swp5.library_management.entity.MaterialRequest;
import com.swp5.library_management.entity.Notification;
import com.swp5.library_management.entity.User;
import com.swp5.library_management.librarian.service.EmailService;
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
                request.getRequestUrgency()
        );

        log.info("[MaterialRequest] Request #{} created for Patron={}. Email confirmation sent to={}",
                savedRequest.getRequestId(), patronId, request.getEmail());

        return savedRequest;
    }
}
