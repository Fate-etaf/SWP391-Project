package com.swp5.library_management.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.swp5.library_management.dto.CampusInventoryDTO;
import com.swp5.library_management.dto.DashboardDataDTO;
import com.swp5.library_management.dto.InventoryOverviewDTO;
import com.swp5.library_management.entity.Campus;
import com.swp5.library_management.repository.BookCopyRepository;
import com.swp5.library_management.repository.BorrowTicketDetailRepository;
import com.swp5.library_management.repository.CampusRepository;
import com.swp5.library_management.repository.TransferRequestRepository;
import com.swp5.library_management.repository.WaitlistRepository;

@Service
public class InventoryServiceImpl implements InventoryService {

    private final BookCopyRepository bookCopyRepository;
    private final CampusRepository campusRepository;

    private final WaitlistRepository waitlistRepository;
    private final BorrowTicketDetailRepository borrowTicketDetailRepository;
    private final TransferRequestRepository transferRequestRepository;

    public InventoryServiceImpl(BookCopyRepository bookCopyRepository,
            CampusRepository campusRepository,
            WaitlistRepository waitlistRepository,
            BorrowTicketDetailRepository borrowTicketDetailRepository,
            TransferRequestRepository transferRequestRepository) {
        this.bookCopyRepository = bookCopyRepository;
        this.campusRepository = campusRepository;
        this.waitlistRepository = waitlistRepository;
        this.borrowTicketDetailRepository = borrowTicketDetailRepository;
        this.transferRequestRepository = transferRequestRepository;
    }

    @Override
    public InventoryOverviewDTO getStats(Integer campusId, Integer categoryId, String fromDate, String toDate) {
        // parse date range if provided (expecting YYYY-MM-DD)
        LocalDateTime from = null;
        LocalDateTime to = null;
        try {
            if (fromDate != null && !fromDate.isBlank()) {
                LocalDate ld = LocalDate.parse(fromDate);
                from = ld.atStartOfDay();
            }
            if (toDate != null && !toDate.isBlank()) {
                LocalDate ld2 = LocalDate.parse(toDate);
                to = ld2.atTime(23, 59, 59);
            }
        } catch (DateTimeParseException ex) {
            // ignore parse errors and treat as nulls
            from = null;
            to = null;
        }

        List<Campus> campuses = campusRepository.findAll();
        Map<Integer, CampusInventoryDTO> map = new HashMap<>();
        for (Campus c : campuses) {
            map.put(c.getCampusId(), new CampusInventoryDTO(c.getCampusId(), c.getCampusName(), 0L, 0L, 0L, 0L));
        }

        List<Object[]> rows = bookCopyRepository.countStatusGroupByFilters(campusId, categoryId, from, to);
        for (Object[] row : rows) {
            Integer cid = (Integer) row[0];
            String status = (String) row[1];
            Long count = (Long) row[2];
            CampusInventoryDTO dto = map.get(cid);
            if (dto == null) {
                dto = new CampusInventoryDTO(cid, "(unknown)", 0L, 0L, 0L, 0L);
                map.put(cid, dto);
            }
            dto.setTotalCopies(dto.getTotalCopies() + count);
            switch (status) {
                case "Available":
                    dto.setAvailable(dto.getAvailable() + count);
                    break;
                case "Borrowed":
                    dto.setBorrowed(dto.getBorrowed() + count);
                    break;
                case "Overdue":
                    dto.setOverdue(dto.getOverdue() + count);
                    break;
                default:
            }
        }

        List<CampusInventoryDTO> list = map.values().stream()
                .sorted(Comparator.comparing(CampusInventoryDTO::getCampusName,
                        Comparator.nullsLast(String::compareTo)))
                .collect(Collectors.toList());

        return new InventoryOverviewDTO(list);
    }

    // ========================================================================
    // --- BƯỚC 2: LOGIC TÍNH TOÁN CHO INVENTORY DASHBOARD ---
    // ========================================================================
    @Override
    public DashboardDataDTO getDashboardData(Integer campusId, List<String> subjectCodes, List<String> conditions,
            List<String> statuses) {
        DashboardDataDTO dto = new DashboardDataDTO();

        // 1. TOP CARDS
        dto.setTotalCopies(bookCopyRepository.countTotalCopiesByCampus(campusId));
        dto.setBorrowedCopies(bookCopyRepository.countByCampusCampusIdAndCopyStatus(campusId, "Borrowed"));
        dto.setOverdueCopies(borrowTicketDetailRepository.countCurrentOverdue(campusId));
        dto.setPendingInboundTransfers(transferRequestRepository.countPendingInboundRequests(campusId));

        // 2. DATA TABLES
        // 2.1 Bảng "Điểm nóng Waitlist" & Smart Column Logic (Xuyên cơ sở)
        List<DashboardDataDTO.WaitlistHotspotDTO> hotspots = waitlistRepository.findTopWaitlistHotspots(campusId,
                PageRequest.of(0, 5));
        for (DashboardDataDTO.WaitlistHotspotDTO hotspot : hotspots) {
            List<Object[]> crossCampusStock = bookCopyRepository.countAvailableCrossCampus(hotspot.getBookId(),
                    campusId);

            if (crossCampusStock == null || crossCampusStock.isEmpty()) {
                hotspot.setCrossCampusStockInfo("Hết sách toàn hệ thống");
                hotspot.setCanRequestTransfer(false); // Báo UI hiện nút [Đề xuất Mua Mới]
            } else {
                // Format thành chuỗi: "Đà Nẵng: 2 cuốn, Cần Thơ: 1 cuốn"
                String stockInfo = crossCampusStock.stream()
                        .map(row -> row[0] + ": " + row[1] + " cuốn")
                        .collect(Collectors.joining(", "));
                hotspot.setCrossCampusStockInfo(stockInfo);
                hotspot.setCanRequestTransfer(true); // Báo UI hiện nút [Xin Luân Chuyển]
            }
        }
        dto.setWaitlistHotspots(hotspots);

        // 2.2 Bảng "Sách quá hạn cần thu hồi"
        dto.setOverdueBooks(borrowTicketDetailRepository.findOverdueActionsByCampus(campusId, PageRequest.of(0, 10)));

        // 3. CHARTS DATA (Phân loại bằng Map để trả về JSON chuẩn cho Chart.js)
        boolean hasSubjectCodes = subjectCodes != null && !subjectCodes.isEmpty();
        List<String> safeSubjectCodes = hasSubjectCodes ? subjectCodes : java.util.Arrays.asList("");

        boolean hasConditions = conditions != null && !conditions.isEmpty();
        List<String> safeConditions = hasConditions ? conditions : java.util.Arrays.asList("");

        boolean hasStatuses = statuses != null && !statuses.isEmpty();
        List<String> safeStatuses = hasStatuses ? statuses : java.util.Arrays.asList("");

        List<Object[]> chartRawData = bookCopyRepository.getChartDataByCampusAndFilters(
                campusId, 
                hasSubjectCodes, safeSubjectCodes,
                hasConditions, safeConditions,
                hasStatuses, safeStatuses);

        Map<String, Map<String, Long>> stackedBarData = new HashMap<>();
        Map<String, Long> doughnutData = new HashMap<>();

        for (Object[] row : chartRawData) {
            String subject = row[0] != null ? row[0].toString() : "Khác";
            String status = row[1] != null ? row[1].toString() : "Unknown";
            Long count = (Long) row[2];

            // 3.1 Cấu trúc cho Stacked Bar Chart (Dữ liệu đa chiều)
            stackedBarData.putIfAbsent(subject, new HashMap<>());
            stackedBarData.get(subject).put(status, stackedBarData.get(subject).getOrDefault(status, 0L) + count);

            // 3.2 Cấu trúc cho Doughnut Chart (Cắt lát theo 1 trạng thái cụ thể)
            // Do Data đã được query Database filter theo statuses nên không cần check lại
            doughnutData.put(subject, doughnutData.getOrDefault(subject, 0L) + count);
        }

        dto.setStackedBarData(stackedBarData);
        dto.setDoughnutData(doughnutData);

        return dto;
    }

    @Override
    public InventoryOverviewDTO getOverview() {
        // initialize campus DTOs
        List<Campus> campuses = campusRepository.findAll();
        Map<Integer, CampusInventoryDTO> map = new HashMap<>();
        for (Campus c : campuses) {
            map.put(c.getCampusId(), new CampusInventoryDTO(c.getCampusId(), c.getCampusName(), 0L, 0L, 0L, 0L));
        }

        // aggregate counts by campus and status
        List<Object[]> rows = bookCopyRepository.countStatusGroupByCampusAll();
        for (Object[] row : rows) {
            // row: [campusId, copyStatus, count]
            Integer campusId = (Integer) row[0];
            String status = (String) row[1];
            Long count = (Long) row[2];
            CampusInventoryDTO dto = map.get(campusId);
            if (dto == null) {
                dto = new CampusInventoryDTO(campusId, "(unknown)", 0L, 0L, 0L, 0L);
                map.put(campusId, dto);
            }
            dto.setTotalCopies(dto.getTotalCopies() + count);
            switch (status) {
                case "Available":
                    dto.setAvailable(dto.getAvailable() + count);
                    break;
                case "Borrowed":
                    dto.setBorrowed(dto.getBorrowed() + count);
                    break;
                case "Overdue":
                    dto.setOverdue(dto.getOverdue() + count);
                    break;
                default:
                    // other statuses ignored for now
            }
        }

        List<CampusInventoryDTO> list = map.values().stream()
                .sorted(Comparator.comparing(CampusInventoryDTO::getCampusName,
                        Comparator.nullsLast(String::compareTo)))
                .collect(Collectors.toList());

        return new InventoryOverviewDTO(list);
    }
}
