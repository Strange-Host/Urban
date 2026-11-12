package com.devsam.urbantrend.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "leads")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Lead {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String phoneNumber;

    private String alternativePhoneNumber;

    private String location;

    private String notes;

    @Column( name = "additional_info")
    private String additionalInfo;

    private Integer formId;

    private String formName;

    @Column(columnDefinition = "TEXT")
    private String rawData;

    private Long wpEntryId;

    private LocalDateTime created;

    private LocalDateTime followUpAt;

    @Column(name = "notified")
    private boolean notified = false;

    private String status;

    @Column(columnDefinition = "TEXT")
    private String productOptions;

    @ManyToOne
    @JoinColumn(name = "assigned_to")
    private User assignedTo;

    @Column(name = "converted_order_id")
    private java.util.UUID convertedOrderId;

    @Column(name = "shopify_order_id")
    private Long shopifyOrderId;

    @PrePersist
    protected void onCreate() {
        this.created = LocalDateTime.now();
        this.status = "NEW";
    }
}