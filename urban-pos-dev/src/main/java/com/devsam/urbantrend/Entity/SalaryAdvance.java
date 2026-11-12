package com.devsam.urbantrend.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name="salary_advances")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalaryAdvance extends BaseEntity{

    @OneToOne(optional=false)
    @JoinColumn(name="requisition_id")
    private Requisition requisition;

    @ManyToOne(optional=false)
    @JoinColumn(name="user_id")
    private User user;

    @Column(nullable=false)
    private BigDecimal amount;

    @Column(columnDefinition="TEXT")
    private String note;
}

