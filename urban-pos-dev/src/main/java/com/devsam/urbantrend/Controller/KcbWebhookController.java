package com.devsam.urbantrend.Controller;

import com.devsam.urbantrend.Dtos.*;
import com.devsam.urbantrend.Service.Auth.KcbIpnService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@RestController
@RequestMapping("/api/v1/webhooks")
@RequiredArgsConstructor
public class KcbWebhookController {

    private static final Logger log = LoggerFactory.getLogger(KcbWebhookController.class);

    private final KcbIpnService kcbIpnService;

    @Value("${kcb.ipn.secret:}")
    private String ipnSecret;

    @PostMapping("/kcb/ipn")
    public ResponseEntity<KcbIpnResponse> receiveIpn(
            @RequestParam(name = "token", required = false) String token,
            @RequestHeader(name = "X-KCB-IPN-Secret", required = false) String headerSecret,
            @RequestBody KcbIpnRequest request) {

        if (ipnSecret == null || ipnSecret.isBlank()) {
            log.error("KCB IPN rejected: kcb.ipn.secret is not configured");
            return ResponseEntity.status(503).build();
        }

        String provided = token != null ? token : headerSecret;

        if (provided == null || !MessageDigest.isEqual(
                provided.getBytes(StandardCharsets.UTF_8),
                ipnSecret.getBytes(StandardCharsets.UTF_8))) {
            log.warn("KCB IPN rejected: invalid shared secret");
            return ResponseEntity.status(403).build();
        }

        if (request.getRequestPayload() == null
                || request.getRequestPayload().getAdditionalData() == null
                || request.getRequestPayload().getAdditionalData().getNotificationData() == null) {
            return ResponseEntity.badRequest().build();
        }

        kcbIpnService.handleIpn(request);

        String messageId = request.getHeader() == null ? null : request.getHeader().getMessageID();
        String originatorConversationId = request.getHeader() == null
                ? null
                : request.getHeader().getOriginatorConversationID();

        String transactionId = request.getRequestPayload()
                .getAdditionalData()
                .getNotificationData()
                .getTransactionID();

        ResponseHeader header = new ResponseHeader(
                messageId,
                originatorConversationId,
                "0",
                "Notification received successfully"
        );

        TransactionInfo transactionInfo = new TransactionInfo(transactionId);
        ResponsePayload responsePayload = new ResponsePayload(transactionInfo);

        KcbIpnResponse response = new KcbIpnResponse(header, responsePayload);

        return ResponseEntity.ok(response);
    }
}
