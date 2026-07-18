package com.swp5.library_management.service;

import com.swp5.library_management.entity.User;
import com.swp5.library_management.repository.BorrowTicketDetailRepository;
import com.swp5.library_management.repository.FineInvoiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserStatusServiceImpl implements UserStatusService {

    private final FineInvoiceRepository fineInvoiceRepository;
    private final BorrowTicketDetailRepository borrowTicketDetailRepository;
    
    // Giới hạn mượn mặc định (nếu không có SystemConfig)
    private static final int MAX_BORROW_LIMIT = 5;

    @Override
    @Transactional(readOnly = true)
    public void enrichStatuses(List<User> users) {
        if (users == null || users.isEmpty()) return;

        List<String> userIds = users.stream().map(User::getUserId).collect(Collectors.toList());

        List<Object[]> unpaidFinesData = fineInvoiceRepository.countUnpaidFinesByUsers(userIds);
        List<Object[]> overdueData = borrowTicketDetailRepository.countOverdueByUsers(userIds, LocalDateTime.now());
        List<Object[]> activeBorrowData = borrowTicketDetailRepository.countActiveBorrowedByUsers(userIds);

        Map<String, Long> unpaidMap = parseCountMap(unpaidFinesData);
        Map<String, Long> overdueMap = parseCountMap(overdueData);
        Map<String, Long> activeBorrowMap = parseCountMap(activeBorrowData);

        for (User user : users) {
            String id = user.getUserId();
            long unpaid = unpaidMap.getOrDefault(id, 0L);
            long overdue = overdueMap.getOrDefault(id, 0L);
            long active = activeBorrowMap.getOrDefault(id, 0L);

            user.setUnpaidFinesCount(unpaid);
            user.setOverdueCount(overdue);
            user.setActiveBorrowCount(active);

            String status = determineStatus(user.getStatus(), unpaid, overdue, active, user.getBorrowingLocked());
            user.setComputedStatus(status);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public String calculateSingleStatus(String userId, String dbStatus) {
        List<Object[]> unpaidFinesData = fineInvoiceRepository.countUnpaidFinesByUsers(List.of(userId));
        List<Object[]> overdueData = borrowTicketDetailRepository.countOverdueByUsers(List.of(userId), LocalDateTime.now());
        List<Object[]> activeBorrowData = borrowTicketDetailRepository.countActiveBorrowedByUsers(List.of(userId));

        long unpaid = parseCountMap(unpaidFinesData).getOrDefault(userId, 0L);
        long overdue = parseCountMap(overdueData).getOrDefault(userId, 0L);
        long active = parseCountMap(activeBorrowData).getOrDefault(userId, 0L);

        return determineStatus(dbStatus, unpaid, overdue, active, false); // Giả sử chưa check borrowingLocked cứng
    }

    private String determineStatus(String dbStatus, long unpaid, long overdue, long active, Boolean borrowingLocked) {
        if ("Graduated".equalsIgnoreCase(dbStatus)) {
            return "Graduated";
        }
        if (Boolean.TRUE.equals(borrowingLocked)) {
            return "Borrowing Locked";
        }
        if (unpaid > 0) {
            return "Under Penalty";
        }
        if (overdue > 0) {
            return "Overdue";
        }
        if (active >= MAX_BORROW_LIMIT) {
            return "Limit Reached";
        }
        if ("Inactive".equalsIgnoreCase(dbStatus)) {
            return "Inactive";
        }
        return "Active";
    }

    private Map<String, Long> parseCountMap(List<Object[]> data) {
        return data.stream().collect(Collectors.toMap(
                row -> (String) row[0],
                row -> ((Number) row[1]).longValue()
        ));
    }
}
