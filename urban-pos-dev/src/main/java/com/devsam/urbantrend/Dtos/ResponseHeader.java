package com.devsam.urbantrend.Dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ResponseHeader {
    private String messageID;
    private String originatorConversationID;
    private String statusCode;
    private String statusMessage;
}