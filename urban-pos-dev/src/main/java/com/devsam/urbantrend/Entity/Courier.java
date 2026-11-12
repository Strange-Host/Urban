package com.devsam.urbantrend.Entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Builder
@Getter
@Setter
@Table(name = "couriers")
@NoArgsConstructor
@AllArgsConstructor
public class Courier extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(name = "phone_number", nullable = false)
    private String phoneNumber;

    @Column(name = "vehicle_info")
    private String vehicleInfo;

    @Column(columnDefinition = "text")
    private String notes;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;
}