package com.devsam.urbantrend.Dtos;

import lombok.Builder;
import lombok.Data;

@Data  @Builder
public class SendMessageRequest {
    private String[] to;
    private String message;
    private String from;
}
