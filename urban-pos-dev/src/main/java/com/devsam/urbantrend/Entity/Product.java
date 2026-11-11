package com.devsam.urbantrend.Entity;

import com.devsam.urbantrend.Enum.ProductStatus;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product extends BaseEntity{

    @Column(nullable = false)
    private String name;

    @Column(unique = true)
    private String sku;

    @Column(nullable = false)
    private BigDecimal sellingPrice;

    @Column(nullable = false)
    private BigDecimal costPrice;

    @ManyToOne
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Enumerated(EnumType.STRING)
    private ProductStatus status;

    @ManyToOne
    @JoinColumn(name = "assigned_staff_id")
    private User assignedStaff;

}