package com.swp5.library_management.repository;

import com.swp5.library_management.entity.BorrowTicketDetail;
import com.swp5.library_management.entity.FineInvoice;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

/**
 * Repository riêng cho tính năng kiểm tra nghĩa vụ tốt nghiệp.
 */
@Repository
public class GraduationRepository {

    @PersistenceContext
    private EntityManager em;

    /**
     * Lấy danh sách sách chưa trả của nhóm sinh viên.
     */
    public List<BorrowTicketDetail> findUnreturnedByUserIds(List<String> userIds) {
        if (userIds == null || userIds.isEmpty()) return new ArrayList<>();

        return em.createQuery(
                "SELECT d FROM BorrowTicketDetail d " +
                "LEFT JOIN FETCH d.bookCopy c " +
                "LEFT JOIN FETCH c.book b " +
                "WHERE d.borrowTicket.patron.userId IN :userIds " +
                "AND d.returnDate IS NULL " +
                "AND (d.status IS NULL OR d.status NOT IN ('Lost', 'Damaged'))", BorrowTicketDetail.class)
            .setParameter("userIds", userIds)
            .getResultList();
    }

    /**
     * Lấy danh sách phiếu phạt chưa thanh toán của nhóm sinh viên.
     */
    public List<FineInvoice> findUnpaidFinesByUserIds(List<String> userIds) {
        if (userIds == null || userIds.isEmpty()) return new ArrayList<>();

        return em.createQuery(
                "SELECT f FROM FineInvoice f " +
                "LEFT JOIN FETCH f.ticketDetail d " +
                "LEFT JOIN FETCH d.bookCopy c " +
                "LEFT JOIN FETCH c.book b " +
                "WHERE f.patron.userId IN :userIds " +
                "AND (f.paidStatus IS NULL OR f.paidStatus <> 'PAID')", FineInvoice.class)
            .setParameter("userIds", userIds)
            .getResultList();
    }
}
