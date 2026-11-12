package com.devsam.urbantrend.Enum;

public enum OrderStatus {
    AWAITING_DISPATCH,
    DISPATCHED_PAID,
    DISPATCHED_PARTIAL,
    DISPATCHED_UNPAID,
    DELIVERED_PAID,
    DELIVERED_UNPAID,
    RETURN_REQUESTED,
    RETURN_IN_TRANSIT,
    RETURNED_AWAITING_INSPECTION,
    RETURNED_GOOD,
    RETURNED_DAMAGED,
    CANCELLED,
    COMPLETED;

    public boolean isRevenueRecognized() {
        return this == DISPATCHED_PAID
                || this == DISPATCHED_PARTIAL
                || this == DISPATCHED_UNPAID
                || this == DELIVERED_PAID
                || this == DELIVERED_UNPAID
                || this == COMPLETED;
    }

    public boolean isFullyPaid() {
        return this == DISPATCHED_PAID
                || this == DELIVERED_PAID
                || this == COMPLETED;
    }

    public boolean isDeliveredOrClosed() {
        return this == DELIVERED_PAID
                || this == DELIVERED_UNPAID
                || this == COMPLETED;
    }
}
