package com.devsam.urbantrend.Service.Auth;

import com.devsam.urbantrend.Dtos.LeadResponse;
import com.devsam.urbantrend.Entity.Lead;
import com.devsam.urbantrend.Entity.Product;
import com.devsam.urbantrend.Entity.User;
import com.devsam.urbantrend.Errors.ResourceNotFoundException;
import com.devsam.urbantrend.Repository.LeadRepository;
import com.devsam.urbantrend.Repository.ProductRepository;
import com.devsam.urbantrend.Roles.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LeadService {

    private static final Set<String> ALLOWED_STATUSES =
            Set.of("NEW", "CONTACTED", "RESCHEDULED", "CANCELLED", "CONVERTED");

    private static final Set<String> EDITABLE_FIELDS =
            Set.of("notes", "alternativePhoneNumber", "location", "additionalInfo", "formName");

    private final LeadRepository leadRepository;
    private final ProductRepository productRepository;

    public Page<LeadResponse> getAll(String status, Pageable pageable, User user) {

        boolean canViewAllLeads =
                user.getRole() == UserRole.ADMIN || user.getRole() == UserRole.MANAGER;

        String normalizedStatus = (status == null || status.isBlank())
                ? null
                : status.trim().toUpperCase();
        boolean hasStatusFilter = normalizedStatus != null;

        if (canViewAllLeads) {
            return hasStatusFilter
                    ? leadRepository.findByStatus(normalizedStatus, pageable).map(LeadResponse::from)
                    : leadRepository.findAll(pageable).map(LeadResponse::from);
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
                    ? leadRepository.findByAssignedToAndStatus(user, normalizedStatus, pageable).map(LeadResponse::from)
                    : leadRepository.findByAssignedTo(user, pageable).map(LeadResponse::from);
        }

        return hasStatusFilter
                ? leadRepository.findByAssignedToOrFormNameInAndStatus(user, productNames, normalizedStatus, pageable)
                        .map(LeadResponse::from)
                : leadRepository.findByAssignedToOrFormNameIn(user, productNames, pageable).map(LeadResponse::from);
    }

    public void updateStatus(Long id, String status, String followUpAt) {

        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException("status is required");
        }

        String normalizedStatus = status.trim().toUpperCase();

        if (!ALLOWED_STATUSES.contains(normalizedStatus)) {
            throw new IllegalArgumentException(
                    "status must be one of " + ALLOWED_STATUSES
            );
        }

        Lead lead = findLead(id);

        lead.setStatus(normalizedStatus);

        if ("RESCHEDULED".equals(normalizedStatus)) {
            if (followUpAt == null || followUpAt.isBlank()) {
                throw new IllegalArgumentException("followUpAt is required when status is RESCHEDULED");
            }
            try {
                lead.setFollowUpAt(LocalDateTime.parse(followUpAt.trim()));
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException(
                        "followUpAt must be an ISO date-time such as 2026-01-31T14:30"
                );
            }
        } else {
            lead.setFollowUpAt(null);
        }

        leadRepository.save(lead);
    }

    public void updateLead(Long id, Map<String, Object> updates) {

        if (updates == null || updates.isEmpty()) {
            throw new IllegalArgumentException("No fields supplied for update");
        }

        Set<String> unsupported = updates.keySet()
                .stream()
                .filter(field -> !EDITABLE_FIELDS.contains(field))
                .collect(Collectors.toSet());

        if (!unsupported.isEmpty()) {
            throw new IllegalArgumentException("Unsupported lead fields: " + unsupported);
        }

        Lead lead = findLead(id);

        if (updates.containsKey("notes")) {
            lead.setNotes(toStringOrNull(updates.get("notes")));
        }
        if (updates.containsKey("alternativePhoneNumber")) {
            lead.setAlternativePhoneNumber(toStringOrNull(updates.get("alternativePhoneNumber")));
        }
        if (updates.containsKey("location")) {
            lead.setLocation(toStringOrNull(updates.get("location")));
        }
        if (updates.containsKey("additionalInfo")) {
            lead.setAdditionalInfo(toStringOrNull(updates.get("additionalInfo")));
        }
        if (updates.containsKey("formName")) {
            lead.setFormName(toStringOrNull(updates.get("formName")));
        }

        leadRepository.save(lead);
    }

    private Lead findLead(Long id) {
        return leadRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lead not found with id " + id));
    }

    private String toStringOrNull(Object value) {
        if (value == null) {
            return null;
        }
        String text = value.toString().trim();
        return text.isEmpty() ? null : text;
    }
}