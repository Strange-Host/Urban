package com.devsam.urbantrend.Service.Auth;

import com.devsam.urbantrend.Entity.Lead;
import com.devsam.urbantrend.Repository.LeadRepository;
import com.devsam.urbantrend.Repository.ProductRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ShopifyService {

    private final LeadRepository leadRepository;
    private final ProductRepository productRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public void processShopifyOrder(Map<String, Object> payload) {

        System.out.println("📦 Shopify webhook received: " + payload);

        Map<String, Object> customerData    = (Map<String, Object>) payload.get("customer");
        Map<String, Object> billingAddress  = (Map<String, Object>) payload.get("billing_address");
        Map<String, Object> shippingAddress = (Map<String, Object>) payload.get("shipping_address");

        String name  = "Shopify Customer";
        String phone = "UNKNOWN";
        String city  = "Shopify";

        // ── 1. Try customer block ──────────────────────────────────────────
        if (customerData != null) {
            String firstName = (String) customerData.getOrDefault("first_name", "");
            String lastName  = (String) customerData.getOrDefault("last_name",  "");
            String resolved  = (firstName + " " + lastName).trim();
            if (!resolved.isEmpty()) name = resolved;

            Object phoneObj = customerData.get("phone");
            if (phoneObj != null && !phoneObj.toString().isBlank()) {
                phone = phoneObj.toString();
            }
        }

        // ── 2. Fallback → billing address ─────────────────────────────────
        if (billingAddress != null) {
            if (name.equals("Shopify Customer")) {
                String fn = (String) billingAddress.getOrDefault("first_name", "");
                String ln = (String) billingAddress.getOrDefault("last_name",  "");
                String resolved = (fn + " " + ln).trim();
                if (!resolved.isEmpty()) name = resolved;
            }
            if (phone.equals("UNKNOWN")) {
                Object phoneObj = billingAddress.get("phone");
                if (phoneObj != null && !phoneObj.toString().isBlank()) {
                    phone = phoneObj.toString();
                }
            }
            Object cityObj = billingAddress.get("city");
            if (cityObj != null && !cityObj.toString().isBlank()) {
                city = cityObj.toString();
            }
        }

        // ── 3. Fallback → shipping address ────────────────────────────────
        if (shippingAddress != null) {
            if (name.equals("Shopify Customer")) {
                String fn = (String) shippingAddress.getOrDefault("first_name", "");
                String ln = (String) shippingAddress.getOrDefault("last_name",  "");
                String resolved = (fn + " " + ln).trim();
                if (!resolved.isEmpty()) name = resolved;
            }
            if (phone.equals("UNKNOWN")) {
                Object phoneObj = shippingAddress.get("phone");
                if (phoneObj != null && !phoneObj.toString().isBlank()) {
                    phone = phoneObj.toString();
                }
            }
            if (city.equals("Shopify")) {
                Object cityObj = shippingAddress.get("city");
                if (cityObj != null && !cityObj.toString().isBlank()) {
                    city = cityObj.toString();
                }
            }
        }

        System.out.println("👤 Resolved — name: " + name + ", phone: " + phone + ", city: " + city);

        // ── 4. Resolve line items & save one lead per item ─────────────────
        List<Map<String, Object>> items = (List<Map<String, Object>>) payload.get("line_items");

        if (items == null || items.isEmpty()) {
            throw new RuntimeException("Shopify order has no line items");
        }

        for (Map<String, Object> item : items) {

            try {
                String productName = (String) item.get("title");

                if (productName == null || productName.isBlank()) {
                    System.out.println("⚠️ Skipping item with no title");
                    continue;
                }


                com.devsam.urbantrend.Entity.Product product = productRepository
                        .findFirstByNameIgnoreCase(productName)
                        .orElse(null);

                Lead lead = new Lead();
                lead.setName(name);
                lead.setPhoneNumber(phone);
                lead.setLocation(city);
                lead.setFormName(productName);   // ← convertLead() uses this to find the product
                lead.setStatus("NEW");           // ← staff will manually convert

                if (product != null && product.getAssignedStaff() != null) {
                    lead.setAssignedTo(product.getAssignedStaff());
                }

                lead.setRawData(objectMapper.writeValueAsString(payload));

                leadRepository.save(lead);
                System.out.println("📋 Lead saved | Product: " + productName + " | Customer: " + name
                        + (product == null ? " (product not in DB yet)" : ""));

            } catch (Exception e) {
                System.out.println("⚠️ Failed to save lead for item: " + item + " — " + e.getMessage());
            }
        }

        System.out.println("✅ Shopify leads saved successfully for: " + name);
    }
}