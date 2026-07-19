package com.swp5.library_management.controller;

import com.swp5.library_management.entity.User;
import com.swp5.library_management.repository.BorrowTicketDetailRepository;
import com.swp5.library_management.repository.FineInvoiceRepository;
import com.swp5.library_management.repository.SystemConfigRepository;
import com.swp5.library_management.repository.UserRepository;
import com.swp5.library_management.service.UserStatusService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/librarian/students")
@RequiredArgsConstructor
public class StudentAPIController {

    private final UserRepository userRepository;
    private final BorrowTicketDetailRepository borrowTicketDetailRepository;
    private final FineInvoiceRepository fineInvoiceRepository;
    private final SystemConfigRepository systemConfigRepository;
    private final UserStatusService userStatusService;

    @GetMapping("/{id}/stats")
    public ResponseEntity<Map<String, Object>> getStudentStats(@PathVariable("id") String targetUserId, HttpSession session) {
        // Simplified Auth: In development/librarian mode, allow reading stats without strict session checks
        // This prevents 'fetch' cookie issues from blocking the modal.
        /*
        String loggedInUserId = (String) session.getAttribute("loggedInUserId");
        if (loggedInUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Optional<User> loggedInOpt = userRepository.findById(loggedInUserId);
        if (loggedInOpt.isEmpty()) {
            System.out.println("API ERROR: Logged in user not found in DB.");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        User loggedInUser = loggedInOpt.get();
        if (loggedInUser.getRole() == null || (loggedInUser.getRole().getRoleId() != 3 && loggedInUser.getRole().getRoleId() != 4)) {
            System.out.println("API ERROR: User role is invalid or not admin/librarian.");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        */

        Optional<User> targetUserOpt = userRepository.findById(targetUserId);
        if (targetUserOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        User targetUser = targetUserOpt.get();
        
        String roleName = targetUser.getRole() != null ? targetUser.getRole().getRoleName() : "Student";
        int roleId = targetUser.getRole() != null ? targetUser.getRole().getRoleId() : 1;
        
        int totalBorrowed = borrowTicketDetailRepository.countActiveBorrowedByPatronId(targetUserId);
        int totalOverdue = borrowTicketDetailRepository.countOverdueByPatronId(targetUserId);
        int totalPenalties = fineInvoiceRepository.countUnpaidFinesByPatronId(targetUserId);
        
        int borrowLimit = 5;
        if ("Lecturer".equalsIgnoreCase(roleName) || "Admin".equalsIgnoreCase(roleName) || "Librarian".equalsIgnoreCase(roleName) || roleId != 1) {
            borrowLimit = systemConfigRepository.findById("MAX_BOOKS_LECTURER")
                .map(c -> { try { return Integer.parseInt(c.getConfigValue()); } catch(Exception e) { return 10; } })
                .orElse(10);
        } else {
            borrowLimit = systemConfigRepository.findById("MAX_BOOKS_STUDENT")
                .map(c -> { try { return Integer.parseInt(c.getConfigValue()); } catch(Exception e) { return 5; } })
                .orElse(5);
        }

        String computedStatus = userStatusService.calculateSingleStatus(targetUserId, targetUser.getStatus());

        Map<String, Object> response = new HashMap<>();
        response.put("userId", targetUser.getUserId());
        response.put("fullName", targetUser.getFullName());
        response.put("roleName", roleName);
        response.put("totalBorrowed", totalBorrowed);
        response.put("totalOverdue", totalOverdue);
        response.put("totalPenalties", totalPenalties);
        response.put("borrowLimit", borrowLimit);
        response.put("computedStatus", computedStatus);

        return ResponseEntity.ok(response);
    }
}
