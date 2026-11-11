package com.devsam.urbantrend.Entity;


import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "customers")
public class Customer extends BaseEntity {
    private String name;
    private String phone;
    private String email;
    private String city_town;
}
