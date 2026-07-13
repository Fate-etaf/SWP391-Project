package com.swp5.library_management.repository.specification;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import com.swp5.library_management.dto.TransferFilterDTO;
import com.swp5.library_management.entity.TransferRequest;

import jakarta.persistence.criteria.Predicate;

public class TransferSpecification {

    public static Specification<TransferRequest> buildFilter(TransferFilterDTO filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Filter Date: History only shows Received, Cancelled/Rejected.
            // If they are in history, we typically look at requestedAt or shippedAt.
            if (filter.getStartDate() != null && filter.getEndDate() != null) {
                predicates.add(cb.between(
                        root.get("requestedAt"),
                        filter.getStartDate().atStartOfDay(),
                        filter.getEndDate().atTime(23, 59, 59)));
            }

            if (filter.getFromCampusId() != null) {
                predicates.add(cb.equal(root.get("fromCampus").get("campusId"), filter.getFromCampusId()));
            }

            if (filter.getToCampusId() != null) {
                predicates.add(cb.equal(root.get("toCampus").get("campusId"), filter.getToCampusId()));
            }

            if (StringUtils.hasText(filter.getStatus())) {
                predicates.add(cb.equal(root.get("status"), filter.getStatus().trim()));
            } else {
                // By default, History only shows completed/finalized states
                predicates.add(root.get("status").in("Received", "Rejected", "Cancelled"));
            }

            if (StringUtils.hasText(filter.getCopyId())) {
                jakarta.persistence.criteria.Join<Object, Object> detailsJoin = root.join("details");
                predicates.add(cb.equal(detailsJoin.get("bookCopy").get("copyId"), filter.getCopyId().trim()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
