package com.devsam.urbantrend.Service.Auth;

import com.devsam.urbantrend.Dtos.ScanRequest;
import com.devsam.urbantrend.Dtos.ScanResponse;
import com.devsam.urbantrend.Entity.User;

import java.util.UUID;

public interface DispatchScanService {

    ScanResponse scan(ScanRequest request, User user);

    /**
     * Dispatches an order whose units are all packed but which was not dispatched
     * automatically (for example the last two stickers were scanned at the same moment).
     */
    ScanResponse completeDispatch(UUID orderId, User user);

    // Undo wr scan before the order is dispatched
    void unscan(String barcode);
}
