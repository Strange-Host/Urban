package com.devsam.urbantrend.Entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "notifications")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "message", columnDefinition = "text")
    private String message;

    @Column(name = "type")
    private String type;

    @Column(name = "reference_id")
    private Long referenceId;

    @JsonProperty("read")
    @Column(name = "is_read")
    private boolean isRead = false;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;
}