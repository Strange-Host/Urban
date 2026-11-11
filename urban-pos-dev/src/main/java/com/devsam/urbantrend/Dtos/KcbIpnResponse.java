package com.devsam.urbantrend.Dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class KcbIpnResponse {
    private ResponseHeader header;
    private ResponsePayload responsePayload;
}
