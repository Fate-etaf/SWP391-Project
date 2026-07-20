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

            // 2. Lọc theo Khoảng thời gian và Loại giao dịch (Mượn/Trả)
            if (filter.getStartDate() != null && filter.getEndDate() != null) {
                if ("RETURN".equalsIgnoreCase(filter.getTransactionType())) {
                    // Nếu là giao dịch trả: Ngày lọc áp dụng cho returnDate
                    predicates.add(cb.between(
                            root.get("returnDate"),
                            filter.getStartDate().atStartOfDay(),
                            filter.getEndDate().atTime(23, 59, 59)));
                    // Ép buộc chỉ lấy các bản ghi đã trả sách
                    predicates.add(cb.isNotNull(root.get("returnDate")));
                } else {
                    // Mặc định (ALL hoặc BORROW): Ngày lọc áp dụng cho createdAt của Ticket
                    predicates.add(cb.between(
                            root.get("borrowTicket").get("createdAt"),
                            filter.getStartDate().atStartOfDay(),
                            filter.getEndDate().atTime(23, 59, 59)));
                }
            } else if ("RETURN".equalsIgnoreCase(filter.getTransactionType())) {
                // Kể cả không có date filter, nếu chọn trả thì bắt buộc returnDate != null
                predicates.add(cb.isNotNull(root.get("returnDate")));
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

            // 6. Lọc theo Môn học
            if (StringUtils.hasText(filter.getSubjectCode())) {
                predicates.add(cb.equal(root.get("bookCopy").get("book").get("subject").get("subjectCode"), filter.getSubjectCode().trim()));
            }

            // 7. Lọc theo Chuyên ngành (Sử dụng Subquery để xử lý lỗi Mapping 1 chiều)
            if (filter.getMajorId() != null) {
                jakarta.persistence.criteria.Subquery<String> subquery = query.subquery(String.class);
                jakarta.persistence.criteria.Root<com.swp5.library_management.entity.Major> majorRoot = subquery.from(com.swp5.library_management.entity.Major.class);
                jakarta.persistence.criteria.Join<com.swp5.library_management.entity.Major, com.swp5.library_management.entity.Subject> subjectsJoin = majorRoot.join("subjects");
                subquery.select(subjectsJoin.get("subjectCode"))
                        .where(cb.equal(majorRoot.get("majorId"), filter.getMajorId()));
                
                predicates.add(cb.in(root.get("bookCopy").get("book").get("subject").get("subjectCode")).value(subquery));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}