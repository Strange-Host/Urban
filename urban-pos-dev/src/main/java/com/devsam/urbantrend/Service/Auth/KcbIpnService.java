package com.devsam.urbantrend.Service.Auth;

import com.devsam.urbantrend.Dtos.KcbIpnRequest;

public interface KcbIpnService {
    void handleIpn(KcbIpnRequest request);
}
