package com.swp5.library_management.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Check;
import java.time.LocalDateTime;

@SuppressWarnings("deprecation")
@Entity
@Table(name = "MaterialRequests", schema = "dbo")
@Check(constraints = "Status IN ('Pending', 'Approved', 'Rejected', 'Ordered', 'Available', 'Arrived')")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MaterialRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "RequestID")
    private Integer requestId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PatronID", referencedColumnName = "UserID", nullable = false)
    private User patron;

    @Column(name = "Title", nullable = false, length = 300)
    private String title;

    @Column(name = "ISBN", length = 20)
    private String isbn;

    @Column(name = "Author", nullable = false, length = 200)
    private String author;

    @Column(name = "Publisher", length = 150)
    private String publisher;

    @Column(name = "Language", length = 50)
    private String language;

    @Column(name = "BookLink", length = 500)
    private String bookLink;

    @Column(name = "PublishYear")
    private Integer publishYear;

    @Column(name = "Description")
    private String description;

    @Column(name = "Priority", length = 100)
    private String priority;

    @Column(name = "Reason", nullable = false, length = 1000)
    private String reason;

    @Column(name = "Email", nullable = false, length = 255)
    private String email;

    @Column(name = "Status", nullable = false, length = 20)
    @Builder.Default
    private String status = "Pending";

    @Column(name = "Feedback", length = 1000)
    private String feedback;

    @Column(name = "ReviewedBy", length = 50)
    private String reviewedBy;

    @Column(name = "ReviewedAt")
    private LocalDateTime reviewedAt;

    @Column(name = "CreatedAt", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
