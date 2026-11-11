package com.devsam.urbantrend.Service.Auth;

import com.devsam.urbantrend.Dtos.LeadResponse;
import com.devsam.urbantrend.Entity.Lead;
import com.devsam.urbantrend.Entity.Product;
import com.devsam.urbantrend.Entity.User;
import com.devsam.urbantrend.Repository.LeadRepository;
import com.devsam.urbantrend.Repository.ProductRepository;
import com.devsam.urbantrend.Roles.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LeadService {

    private final LeadRepository leadRepository;
    private final ProductRepository productRepository;

    public Page<Lead> getAll(String status, Pageable pageable, User user) {

        boolean canViewAllLeads =
                user.getRole() == UserRole.ADMIN || user.getRole() == UserRole.MANAGER;

        boolean hasStatusFilter = status != null && !status.isBlank();

        if (canViewAllLeads) {
            return hasStatusFilter
                    ? leadRepository.findByStatus(status, pageable)
                    : leadRepository.findAll(pageable);
        }

        // Staff/Cashier see leads directly assigned to them, plus leads that
        // reference any product assigned to them (matched by formName).
        List<Product> assignedProducts =
                productRepository.findAllByDeletedAtIsNullAndAssignedStaff(user);

        List<String> productNames = assignedProducts.stream()
                .map(Product::getName)
                .filter(name -> name != null && !name.isBlank())
                .map(name -> name.toLowerCase())
                .distinct()
                .collect(Collectors.toList());

        if (productNames.isEmpty()) {
            // No products assigned, fall back to direct assignment only.
            return hasStatusFilter
                    ? leadRepository.findByAssignedToAndStatus(user, status, pageable)
                    : leadRepository.findByAssignedTo(user, pageable);
        }

        return hasStatusFilter
                ? leadRepository.findByAssignedToOrFormNameInAndStatus(user, productNames, status, pageable)
                : leadRepository.findByAssignedToOrFormNameIn(user, productNames, pageable);
    }

    public void updateStatus(Long id, String status, String followUpAt) {

        Lead lead = leadRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Lead not found"));

        lead.setStatus(status);

        if (status.equals("RESCHEDULED") && followUpAt != null) {
            lead.setFollowUpAt(LocalDateTime.parse(followUpAt));
        }

        leadRepository.save(lead);
    }

    public void updateNotes(Long id, String notes) {
        Lead lead = leadRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Lead not found"));
        lead.setNotes(notes);
        leadRepository.save(lead);
    }
}