package com.devsam.urbantrend.Controller;

import com.devsam.urbantrend.Dtos.ConvertLeadRequest;
import com.devsam.urbantrend.Dtos.LeadResponse;
import com.devsam.urbantrend.Entity.Lead;
import com.devsam.urbantrend.Entity.User;
import com.devsam.urbantrend.Repository.LeadRepository;
import com.devsam.urbantrend.Repository.ProductRepository;
import com.devsam.urbantrend.Repository.UserRepository;
import com.devsam.urbantrend.Service.Auth.LeadConversionService;
import com.devsam.urbantrend.Service.Auth.LeadService;
import com.devsam.urbantrend.Service.Auth.StaffLogService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/leads")
@RequiredArgsConstructor
public class LeadController {

    private final LeadRepository repository;
    private final ProductRepository productRepository;
    private final LeadService leadService;
    private final LeadConversionService leadConversionService;
    private final UserRepository userRepository;
    private final StaffLogService staffLogService;

    @Value("${webhook.wpforms.secret:}")
    private String webhookSecret;

    @PostMapping
    public ResponseEntity<String> createLead(
            @RequestParam(name = "secret", required = false) String querySecret,
            @RequestHeader(name = "X-WPForms-Secret", required = false) String headerSecret,
            @RequestBody Map<String, Object> payload
    ) throws Exception {

        if (webhookSecret == null || webhookSecret.isBlank()) {
            return ResponseEntity.status(503).body("Webhook secret is not configured");
        }

        String provided = querySecret != null ? querySecret : headerSecret;

        if (provided == null || !MessageDigest.isEqual(
                provided.getBytes(StandardCharsets.UTF_8),
                webhookSecret.getBytes(StandardCharsets.UTF_8))) {
            return ResponseEntity.status(403).body("Invalid webhook secret");
        }

        ObjectMapper mapper = new ObjectMapper();
        Lead lead = new Lead();

        lead.setFormId(parseInteger(payload.get("formId"), "formId"));

        if (payload.get("formName") != null) {
            lead.setFormName(payload.get("formName").toString());
        }

        lead.setWpEntryId(parseLong(payload.get("wpEntryId"), "wpEntryId"));

        if (lead.getWpEntryId() != null && repository.existsByWpEntryId(lead.getWpEntryId())) {
            return ResponseEntity.ok("Duplicate lead ignored.");
        }

        Object rawFields = payload.get("fields");
        Map<String, Object> fields = rawFields instanceof Map
                ? (Map<String, Object>) rawFields
                : null;

        if (fields != null) {
            if (fields.containsKey("Name")) {
                lead.setName(fields.get("Name").toString());
            } else if (fields.containsKey("First") && fields.containsKey("Last")) {
                lead.setName(fields.get("First") + " " + fields.get("Last"));
            }
            if (fields.containsKey("Phone Number")) {
                lead.setPhoneNumber(fields.get("Phone Number").toString());
            }
            if (fields.containsKey("Alternative Phone Number")) {
                lead.setAlternativePhoneNumber(fields.get("Alternative Phone Number").toString());
            }
            if (fields.containsKey("Location")) {
                lead.setLocation(fields.get("Location").toString());
            }
            if(fields.containsKey("Additional information or any Enquiry. (e.g. exact address, number of pieces).")){
                lead.setAdditionalInfo(fields.get("Additional information or any Enquiry. (e.g. exact address, number of pieces).").toString());
            }

            else if (fields.containsKey("Delivery County")) {
                lead.setLocation(fields.get("Delivery County").toString());
            }

            // Capture product variant fields (colors, sizes, materials, etc.)
            // Different WordPress forms use different labels for these, so we
            // match by prefix/keyword rather than an exact key.
            Map<String, Object> variants = new LinkedHashMap<>();
            for (Map.Entry<String, Object> entry : fields.entrySet()) {
                String key = entry.getKey();
                if (key.startsWith("Available Colors")
                        || key.startsWith("Choose colors")
                        || key.startsWith("Available Sizes")
                        || key.startsWith("Choose size")
                        || key.toLowerCase().contains("color")
                        || key.toLowerCase().contains("size")) {
                    variants.put(key, entry.getValue());
                }
            }
            if (!variants.isEmpty()) {
                lead.setProductOptions(mapper.writeValueAsString(variants));
            }

            lead.setRawData(mapper.writeValueAsString(fields));
        }

        // 🔗 If this lead's product (matched by formName) has a staff
        // assigned to it, route the lead straight to that staff.
        if (lead.getFormName() != null) {
            productRepository.findFirstByNameIgnoreCase(lead.getFormName().trim())
                    .ifPresent(product -> {
                        if (product.getAssignedStaff() != null) {
                            lead.setAssignedTo(product.getAssignedStaff());
                        }
                    });
        }

        repository.save(lead);

        // 🔥 LOG (Webhook source, no authenticated user)
        staffLogService.log(
                "SYSTEM",
                "CREATE",
                "LEAD",
                lead.getId().toString(),
                "/api/v1/leads",
                "POST",
                "Lead created from WordPress webhook"
        );

        return ResponseEntity.ok("Lead stored successfully.");
    }

    @PostMapping("/manual")
    @PreAuthorize("hasAuthority('CAN_CREATE_LEADS')")
    public ResponseEntity<?> createManualLead(
            @RequestBody com.devsam.urbantrend.Dtos.ManualLeadRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) throws Exception {

        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (request.getName() == null || request.getName().isBlank()) {
            return ResponseEntity.badRequest().body("Name is required");
        }
        if (request.getPhoneNumber() == null || request.getPhoneNumber().isBlank()) {
            return ResponseEntity.badRequest().body("Phone number is required");
        }

        ObjectMapper mapper = new ObjectMapper();
        Lead lead = new Lead();

        lead.setName(request.getName());
        lead.setPhoneNumber(request.getPhoneNumber());
        lead.setAlternativePhoneNumber(request.getAlternativePhoneNumber());
        lead.setLocation(request.getLocation());
        lead.setFormName(request.getFormName());
        lead.setAdditionalInfo(request.getAdditionalInfo());
        lead.setNotes(request.getNotes());
        lead.setAssignedTo(user);

        if (request.getProductOptions() != null && !request.getProductOptions().isEmpty()) {
            lead.setProductOptions(mapper.writeValueAsString(request.getProductOptions()));
        }

        // Keep a record of how this lead was captured (e.g. CALL, WHATSAPP)
        // alongside a snapshot of what was submitted, same as webhook leads.
        Map<String, Object> rawSnapshot = new LinkedHashMap<>();
        rawSnapshot.put("source", request.getSource() != null ? request.getSource() : "MANUAL");
        rawSnapshot.put("capturedBy", user.getEmail());
        rawSnapshot.put("name", request.getName());
        rawSnapshot.put("phoneNumber", request.getPhoneNumber());
        rawSnapshot.put("alternativePhoneNumber", request.getAlternativePhoneNumber());
        rawSnapshot.put("location", request.getLocation());
        rawSnapshot.put("formName", request.getFormName());
        rawSnapshot.put("additionalInfo", request.getAdditionalInfo());
        rawSnapshot.put("productOptions", request.getProductOptions());
        lead.setRawData(mapper.writeValueAsString(rawSnapshot));

        repository.save(lead);

        staffLogService.log(
                user.getId().toString(),
                "CREATE",
                "LEAD",
                lead.getId().toString(),
                "/api/v1/leads/manual",
                "POST",
                "Lead manually created via " + (request.getSource() != null ? request.getSource() : "MANUAL")
        );

        return ResponseEntity.ok(lead);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('CAN_VIEW_LEADS')")
    public ResponseEntity<Page<LeadResponse>> getAllLeads(
            @RequestParam(required = false) String status,
            @PageableDefault(size = 10, sort = "created", direction = Sort.Direction.DESC)
            Pageable pageable,
            @AuthenticationPrincipal UserDetails userDetails
    ) {

        User user = userRepository.findByEmailAndDeletedAtIsNull(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        return ResponseEntity.ok(leadService.getAll(status, pageable, user));
    }

    @PostMapping("/{id}/convert")
    @PreAuthorize("hasAuthority('CAN_CONVERT_LEADS')")
    public ResponseEntity<?> convertLead(
            @PathVariable Long id,
            @RequestBody ConvertLeadRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        leadConversionService.convertLead(id, request, user);

        // 🔥 LOG (conversion)
        staffLogService.log(
                user.getId().toString(),
                "CONVERT",
                "LEAD",
                id.toString(),
                "/api/v1/leads/" + id + "/convert",
                "POST",
                "Converted lead to customer/order"
        );

        return ResponseEntity.ok("Lead converted successfully");
    }

    @PatchMapping("/{id}/assign")
    @PreAuthorize("hasAuthority('CAN_ASSIGN_LEADS')")
    public ResponseEntity<?> assignLead(
            @PathVariable Long id,
            @RequestBody com.devsam.urbantrend.Dtos.AssignLeadRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        if (request.getStaffId() == null) {
            return ResponseEntity.badRequest().body("staffId is required");
        }

        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        leadConversionService.assignLeadToStaff(id, request.getStaffId());

        // 🔥 LOG (manual assignment)
        staffLogService.log(
                user.getId().toString(),
                "ASSIGN",
                "LEAD",
                id.toString(),
                "/api/v1/leads/" + id + "/assign",
                "PATCH",
                "Manually assigned lead to staff " + request.getStaffId()
        );

        return ResponseEntity.ok("Lead assigned successfully");
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('CAN_UPDATE_LEAD_STATUS')")
    public ResponseEntity<?> updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        String status = body.get("status");
        String followUpAt = body.get("followUpAt");

        leadService.updateStatus(id, status, followUpAt);

        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 🔥 LOG (status update)
        staffLogService.log(
                user.getId().toString(),
                "UPDATE",
                "LEAD",
                id.toString(),
                "/api/v1/leads/" + id + "/status",
                "PATCH",
                "Updated lead status to " + status
        );

        return ResponseEntity.ok().build();
    }
    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('CAN_UPDATE_LEADS')")
    public ResponseEntity<?> patchLead(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        User user = userRepository.findByEmailAndDeletedAtIsNull(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        leadService.updateLead(id, body);

        staffLogService.log(
                user.getId().toString(),
                "UPDATE",
                "LEAD",
                id.toString(),
                "/api/v1/leads/" + id,
                "PATCH",
                "Updated lead fields: " + body.keySet()
        );

        return ResponseEntity.ok().build();
    }

    private Integer parseInteger(Object value, String field) {
        if (value == null || value.toString().isBlank()) {
            return null;
        }
        try {
            return Integer.valueOf(value.toString().trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(field + " must be a whole number");
        }
    }

    private Long parseLong(Object value, String field) {
        if (value == null || value.toString().isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(value.toString().trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(field + " must be a whole number");
        }
    }
}