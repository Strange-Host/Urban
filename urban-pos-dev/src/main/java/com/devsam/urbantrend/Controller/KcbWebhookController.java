package com.devsam.urbantrend.Controller;

import com.devsam.urbantrend.Dtos.*;
import com.devsam.urbantrend.Service.Auth.KcbIpnService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/webhooks")
@RequiredArgsConstructor
public class KcbWebhookController {

    private final KcbIpnService kcbIpnService;

    @PostMapping("/kcb/ipn")
    public ResponseEntity<KcbIpnResponse> receiveIpn(
            @RequestBody KcbIpnRequest request) {

        // Process IPN
        kcbIpnService.handleIpn(request);

        // Extract header values to echo back
        String messageId = request.getHeader().getMessageID();
        String originatorConversationId = request.getHeader().getOriginatorConversationID();

        // Use transaction ID from request as confirmation
        String transactionId = request.getRequestPayload()
                .getAdditionalData()
                .getNotificationData()
                .getTransactionID();

        // Build response header
        ResponseHeader header = new ResponseHeader(
                messageId,
                originatorConversationId,
                "0",
                "Notification received successfully"
        );

        // Build response payload
        TransactionInfo transactionInfo = new TransactionInfo(transactionId);
        ResponsePayload responsePayload = new ResponsePayload(transactionInfo);

        KcbIpnResponse response = new KcbIpnResponse(header, responsePayload);

        return ResponseEntity.ok(response);
    }
}

