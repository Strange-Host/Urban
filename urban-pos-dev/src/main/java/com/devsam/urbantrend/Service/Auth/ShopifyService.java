package com.devsam.urbantrend.Service.Auth;

import com.devsam.urbantrend.Entity.Lead;
import com.devsam.urbantrend.Entity.Product;
import com.devsam.urbantrend.Repository.LeadRepository;
import com.devsam.urbantrend.Repository.ProductRepository;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ShopifyService {

    private static final Logger log = LoggerFactory.getLogger(ShopifyService.class);

    private final LeadRepository leadRepository;
    private final ProductRepository productRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void processShopifyOrder(Map<String, Object> payload) {

        if (payload == null || payload.isEmpty()) {
            throw new IllegalArgumentException("Shopify order payload is empty");
        }

        Long shopifyOrderId = asLong(payload.get("id"));

        if (shopifyOrderId == null) {
            throw new IllegalArgumentException("Shopify order id is required");
        }

        if (leadRepository.existsByShopifyOrderId(shopifyOrderId)) {
            log.info("Shopify order {} already processed; ignoring duplicate webhook", shopifyOrderId);
            return;
        }

        Map<String, Object> customerData = asMap(payload.get("customer"));
        Map<String, Object> billingAddress = asMap(payload.get("billing_address"));
        Map<String, Object> shippingAddress = asMap(payload.get("shipping_address"));

        String name = "Shopify Customer";
        String phone = "UNKNOWN";
        String city = "Shopify";

        if (customerData != null) {
            String resolved = joinName(
                    asString(customerData.get("first_name")),
                    asString(customerData.get("last_name"))
            );
            if (resolved != null) {
                name = resolved;
            }

            String customerPhone = asString(customerData.get("phone"));
            if (customerPhone != null && !customerPhone.isBlank()) {
                phone = customerPhone;
            }
        }

        if (billingAddress != null) {
            if ("Shopify Customer".equals(name)) {
                String resolved = joinName(
                        asString(billingAddress.get("first_name")),
                        asString(billingAddress.get("last_name"))
                );
                if (resolved != null) {
                    name = resolved;
                }
            }

            if ("UNKNOWN".equals(phone)) {
                String billingPhone = asString(billingAddress.get("phone"));
                if (billingPhone != null && !billingPhone.isBlank()) {
                    phone = billingPhone;
                }
            }

            String billingCity = asString(billingAddress.get("city"));
            if (billingCity != null && !billingCity.isBlank()) {
                city = billingCity;
            }
        }

        if (shippingAddress != null) {
            if ("Shopify Customer".equals(name)) {
                String resolved = joinName(
                        asString(shippingAddress.get("first_name")),
                        asString(shippingAddress.get("last_name"))
                );
                if (resolved != null) {
                    name = resolved;
                }
            }

            if ("UNKNOWN".equals(phone)) {
                String shippingPhone = asString(shippingAddress.get("phone"));
                if (shippingPhone != null && !shippingPhone.isBlank()) {
                    phone = shippingPhone;
                }
            }

            if ("Shopify".equals(city)) {
                String shippingCity = asString(shippingAddress.get("city"));
                if (shippingCity != null && !shippingCity.isBlank()) {
                    city = shippingCity;
                }
            }
        }

        List<Object> items = asList(payload.get("line_items"));

        if (items.isEmpty()) {
            throw new IllegalArgumentException("Shopify order has no line items");
        }

        String rawData = toJson(payload);

        for (Object rawItem : items) {
            Map<String, Object> item = rawItem instanceof Map<?, ?> map
                    ? (Map<String, Object>) map
                    : null;

            if (item == null) {
                throw new IllegalArgumentException("Shopify order contains an invalid line item");
            }

            String productName = asString(item.get("title"));

            if (productName == null || productName.isBlank()) {
                throw new IllegalArgumentException("Shopify line item is missing a product title");
            }

            Product product = productRepository
                    .findFirstByNameIgnoreCaseAndDeletedAtIsNull(productName.trim())
                    .orElse(null);

            Lead lead = new Lead();
            lead.setName(name);
            lead.setPhoneNumber(phone);
            lead.setLocation(city);
            lead.setFormName(productName);
            lead.setStatus("NEW");
            lead.setShopifyOrderId(shopifyOrderId);

            if (product != null && product.getAssignedStaff() != null) {
                lead.setAssignedTo(product.getAssignedStaff());
            }

            lead.setRawData(rawData);

            leadRepository.save(lead);

            log.info("Shopify lead saved | order: {} | product: {} | customer: {}{}",
                    shopifyOrderId,
                    productName,
                    name,
                    product == null ? " (product not in DB yet)" : "");
        }
    }

    private Map<String, Object> asMap(Object value) {
        return value instanceof Map<?, ?> map ? (Map<String, Object>) map : null;
    }

    private List<Object> asList(Object value) {
        return value instanceof List<?> list ? (List<Object>) list : List.of();
    }

    private String asString(Object value) {
        return value instanceof String text ? text : null;
    }

    private Long asLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            try {
                return Long.parseLong(text.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private String joinName(String firstName, String lastName) {
        String first = firstName == null ? "" : firstName.trim();
        String last = lastName == null ? "" : lastName.trim();
        String joined = (first + " " + last).trim();
        return joined.isEmpty() ? null : joined;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JacksonException e) {
            throw new IllegalArgumentException("Unable to serialize Shopify payload");
        }
    }
}