package com.devsam.urbantrend.Controller;

import com.devsam.urbantrend.Service.Auth.ShopifyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/shopify")
@RequiredArgsConstructor
public class ShopifyWebhookController {

    private final ShopifyService shopifyService;

    @PostMapping("/orders")
    public ResponseEntity<Void> receiveOrder(@RequestBody Map<String, Object> payload) {

        shopifyService.processShopifyOrder(payload);

        return ResponseEntity.ok().build();
    }
}