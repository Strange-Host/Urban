package com.devsam.urbantrend.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "staff_logs")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class StaffLog {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String staffId;

    private String action;

    private String entity;

    private String entityId;

    private String endpoint;

    private String method;

    private LocalDateTime timestamp;

    private String description;
}
