package com.devsam.urbantrend.Controller;

import com.devsam.urbantrend.Service.Auth.ShopifyService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/shopify")
@RequiredArgsConstructor
public class ShopifyWebhookController {

    private final ShopifyService shopifyService;
    private final ObjectMapper objectMapper;

    @Value("${shopify.webhook.secret:}")
    private String webhookSecret;

    @PostMapping("/orders")
    public ResponseEntity<Void> receiveOrder(
            @RequestBody(required = false) String rawBody,
            @RequestHeader(name = "X-Shopify-Hmac-Sha256", required = false) String hmac,
            @RequestHeader(name = "X-Shopify-Topic", required = false) String topic
    ) {

        if (webhookSecret == null || webhookSecret.isBlank()) {
            return ResponseEntity.status(503).build();
        }

        if (rawBody == null || rawBody.isBlank()) {
            throw new IllegalArgumentException("Webhook body is required");
        }

        if (!isValidSignature(rawBody, hmac)) {
            throw new AccessDeniedException("Invalid Shopify webhook signature");
        }

        if (topic == null || !topic.startsWith("orders/")) {
            throw new IllegalArgumentException("Unsupported Shopify webhook topic: " + topic);
        }

        Map<String, Object> payload;
        try {
            payload = objectMapper.readValue(rawBody, new TypeReference<>() {
            });
        } catch (Exception e) {
            throw new IllegalArgumentException("Malformed Shopify webhook payload");
        }

        shopifyService.processShopifyOrder(payload);

        return ResponseEntity.ok().build();
    }

    private boolean isValidSignature(String body, String hmac) {

        if (hmac == null || hmac.isBlank()) {
            return false;
        }

        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));

            byte[] expected = mac.doFinal(body.getBytes(StandardCharsets.UTF_8));
            byte[] provided = Base64.getDecoder().decode(hmac);

            return MessageDigest.isEqual(expected, provided);
        } catch (Exception e) {
            return false;
        }
    }
}