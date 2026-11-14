package com.devsam.urbantrend.Service.Auth.Messaging;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ZettatelSmsProvider implements SmsProvider {

    private static final Logger log = LoggerFactory.getLogger(ZettatelSmsProvider.class);

    @Value("${zettatel.api-key}")
    private String apiKey;

    @Value("${zettatel.user-id}")
    private String userId;
    @Value("${zettatel.password}")
    private String password;

    @Value("${zettatel.sender-id:}")
    private String senderId;

    // Bounded timeouts: an unresponsive SMS gateway must never hang a login or a
    // request that happens to send a message.
    private final RestTemplate restTemplate = buildRestTemplate();

    private static RestTemplate buildRestTemplate() {
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(
                java.net.http.HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(5))
                        .build());
        factory.setReadTimeout(Duration.ofSeconds(10));
        return new RestTemplate(factory);
    }

    @Override
    public void sendSms(String phoneNumber, String message) {

        String formattedPhone = normalizeKenyanPhone(phoneNumber);

        if (formattedPhone == null) {
            log.warn("SMS not sent: missing or unrecognised phone number");
            return;
        }

        try {

            String url = "https://portal.zettatel.com/SMSApi/send";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = new HashMap<>();

            body.put("userid", userId);
            body.put("password", password);
            body.put("senderid", senderId);
            body.put("msgType", "unicode");
            body.put("duplicatecheck", "true");
            body.put("sendMethod", "quick");

            Map<String, Object> sms = new HashMap<>();
            sms.put("mobile", List.of(formattedPhone));
            sms.put("msg", message);

            body.put("sms", List.of(sms));

            HttpEntity<Map<String, Object>> request =
                    new HttpEntity<>(body, headers);

            ResponseEntity<String> response =
                    restTemplate.postForEntity(url, request, String.class);

            log.info("📨 Zettatel SMS SENT TO: {}", formattedPhone);
            log.info("✅ RESPONSE: {}", response.getBody());

        } catch (Exception e) {
            log.error("❌ SMS failed: {}", e.getMessage(), e);
        }
    }
    private String normalizeKenyanPhone(String phone) {
        if (phone == null) {
            return null;
        }
        // Staff and customers type numbers like "0712 345 678" or "0712-345-678".
        phone = phone.trim().replaceAll("[\\s\\-().]", "");

        if (phone.startsWith("0") && phone.length() == 10) {
            return "254" + phone.substring(1);
        }

        if (phone.startsWith("+254")) {
            return phone.substring(1);
        }

        if (phone.startsWith("254")) {
            return phone;
        }

        return null;
    }
}