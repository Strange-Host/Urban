package com.devsam.urbantrend.Service.Auth;

import com.devsam.urbantrend.Dtos.ScanRequest;
import com.devsam.urbantrend.Dtos.ScanResponse;
import com.devsam.urbantrend.Entity.User;

public interface DispatchScanService {

    ScanResponse scan(ScanRequest request, User user);

    // Undo wr scan before the order is dispatched
    void unscan(String barcode);
}
