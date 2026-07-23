package com.swp5.library_management.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "FineInvoices")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FineInvoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "FineID")
    private Integer fineId;

    @ManyToOne
    @JoinColumn(name = "PatronID")
    private User patron;

    @ManyToOne
    @JoinColumn(name = "TicketDetailID")
    private BorrowTicketDetail ticketDetail;

    @Column(name = "FineAmount")
    private BigDecimal fineAmount;

    @Column(name = "RemainingAmount")
    private BigDecimal remainingAmount;

    @Column(name = "ViolationType")
    private String violationType;

    @Column(name = "Reason")
    private String reason;

    @Column(name = "CreatedAt")
    private LocalDateTime createdAt;

    @Column(name = "PaidAt")
    private LocalDateTime paidAt;

    @Column(name = "PaidStatus")
    private String paidStatus;

    @ManyToOne
    @JoinColumn(name = "ProcessedBy")
    private User processedBy;

    /** Phương thức thanh toán: 'Cash', 'QRCode', 'BankTransfer', hoặc NULL nếu chưa thanh toán */
    @Column(name = "PaymentMethod", length = 255)
    private String paymentMethod;

    /** Mã giao dịch tham chiếu (biên lai tiền mặt hoặc mã chuyển khoản) */
    @Column(name = "TransactionCode", length = 255)
    private String transactionCode;
}