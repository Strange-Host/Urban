package com.devsam.urbantrend.Dtos;

import lombok.Data;

@Data
public class KcbIpnRequest {

    private Header header;
    private RequestPayload requestPayload;

    @Data
    public static class Header {
        private String messageID;
        private String originatorConversationID;
        private String channelCode;
        private String timeStamp;
    }

    @Data
    public static class RequestPayload {
        private PrimaryData primaryData;
        private AdditionalData additionalData;
    }

    @Data
    public static class PrimaryData {
        private String businessKey;
        private String businessKeyType;
    }

    @Data
    public static class AdditionalData {
        private NotificationData notificationData;
    }

    @Data
    public static class NotificationData {
        private String businessKey; // this will be 8025631#A01
        private String businessKeyType;
        private String debitMSISDN;
        private String transactionAmt;
        private String transactionDate;
        private String transactionID;
        private String firstName;
        private String middleName;
        private String lastName;
        private String currency;
        private String narration;
        private String transactionType;
        private String balance;
    }
}

