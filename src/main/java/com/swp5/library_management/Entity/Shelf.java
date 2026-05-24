package com.swp5.library_management.Entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "Shelves", schema = "dbo",
        uniqueConstraints = @UniqueConstraint(columnNames = {"CampusID", "ShelfCode"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Shelf {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ShelfID")
    private Integer shelfId;

    @Column(name = "ShelfNumber", nullable = false)
    private Integer shelfNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CampusID", nullable = false)
    private Campus campus;

    @Column(name = "ShelfCode", nullable = false, length = 50)
    private String shelfCode;

    @Column(name = "ShelfName", length = 100)
    private String shelfName;

    @Column(name = "ShelfCodeTopic", length = 100)
    private String shelfCodeTopic;
}
