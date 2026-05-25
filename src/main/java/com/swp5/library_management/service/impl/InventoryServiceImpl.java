package com.swp5.library_management.service.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.swp5.library_management.entity.Campus;
import com.swp5.library_management.repository.BookCopyRepository;
import com.swp5.library_management.repository.CampusRepository;
import com.swp5.library_management.service.InventoryService;
import com.swp5.library_management.service.dto.CampusInventoryDTO;
import com.swp5.library_management.service.dto.InventoryOverviewDTO;

@Service
public class InventoryServiceImpl implements InventoryService {

    private final BookCopyRepository bookCopyRepository;
    private final CampusRepository campusRepository;

    public InventoryServiceImpl(BookCopyRepository bookCopyRepository, CampusRepository campusRepository) {
        this.bookCopyRepository = bookCopyRepository;
        this.campusRepository = campusRepository;
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
                dto = new CampusInventoryDTO(cid, "(unknown)", 0L,0L,0L,0L);
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
                .sorted(Comparator.comparing(CampusInventoryDTO::getCampusName, Comparator.nullsLast(String::compareTo)))
                .collect(Collectors.toList());

        return new InventoryOverviewDTO(list);
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
                dto = new CampusInventoryDTO(campusId, "(unknown)", 0L,0L,0L,0L);
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
                .sorted(Comparator.comparing(CampusInventoryDTO::getCampusName, Comparator.nullsLast(String::compareTo)))
                .collect(Collectors.toList());

        return new InventoryOverviewDTO(list);
    }
}
