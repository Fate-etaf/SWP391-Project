package com.swp5.library_management.repository.specification;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import com.swp5.library_management.dto.ReportFilterDTO;
import com.swp5.library_management.entity.BorrowTicketDetail;

import jakarta.persistence.criteria.Predicate;

public class TransactionSpecification {

    public static Specification<BorrowTicketDetail> buildBorrowFilter(ReportFilterDTO filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. Lọc theo Cơ sở (CampusID)
            if (filter.getCampusId() != null) {
                predicates.add(cb.equal(root.get("borrowTicket").get("campus").get("campusId"), filter.getCampusId()));
            }

            // 2. Lọc theo Khoảng thời gian (Ngày Mượn)
            if (filter.getStartDate() != null && filter.getEndDate() != null) {
                predicates.add(cb.between(
                        root.get("borrowTicket").get("createdAt"),
                        filter.getStartDate().atStartOfDay(),
                        filter.getEndDate().atTime(23, 59, 59)));
            }

            // 3. Lọc theo CopyID (Tra cứu đích danh cuốn sách)
            if (StringUtils.hasText(filter.getCopyId())) {
                predicates.add(cb.equal(root.get("bookCopy").get("copyId"), filter.getCopyId().trim()));
            }

            // 4. Lọc theo UserID (Gộp chung tìm kiếm Sinh viên mượn hoặc Thủ thư trực)
            if (StringUtils.hasText(filter.getUserId())) {
                String keyword = "%" + filter.getUserId().trim().toLowerCase() + "%";
                Predicate matchPatron = cb.like(cb.lower(root.get("borrowTicket").get("patron").get("userId")),
                        keyword);
                Predicate matchLibrarian = cb.like(cb.lower(root.get("borrowTicket").get("librarian").get("userId")),
                        keyword);
                predicates.add(cb.or(matchPatron, matchLibrarian));
            }

            // 5. Lọc theo Trạng thái (Borrowing, Returned, Overdue...)
            if (StringUtils.hasText(filter.getStatus())) {
                predicates.add(cb.equal(root.get("status"), filter.getStatus().trim()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}