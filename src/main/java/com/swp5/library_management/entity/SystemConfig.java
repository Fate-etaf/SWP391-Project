package com.swp5.library_management.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "SystemConfig")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemConfig {

    @Id
    @Column(name = "ConfigKey", length = 100, nullable = false)
    private String configKey;

    @Column(name = "ConfigValue", length = 500, nullable = false)
    private String configValue;

    @Column(name = "Description", length = 300)
    private String description;

    @Column(name = "UpdatedAt", nullable = false)
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "UpdatedBy")
    private User updatedBy;
}
