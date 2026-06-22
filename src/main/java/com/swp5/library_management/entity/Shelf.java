package com.swp5.library_management.entity;

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
    @Column(name = "ShelfCode", nullable = false, length = 50)
    private String shelfCode;

    @Column(name = "ShelfNumber", nullable = false)
    private Integer shelfNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CampusID", nullable = false)
    private Campus campus;

    @Column(name = "ShelfName", length = 100)
    private String shelfName;

    @Column(name = "ShelfCodeTopic", length = 100)
    private String shelfCodeTopic;
}
