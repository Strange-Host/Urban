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

    public boolean isDispatchedFamily() {
        return this == DISPATCHED_PAID
                || this == DISPATCHED_PARTIAL
                || this == DISPATCHED_UNPAID;
    }

    /**
     * Status an order should have after a payment changes its balance.
     * Payments never dispatch an order: only the scan or a manual status change does.
     */
    public OrderStatus afterPayment(boolean fullyPaid) {
        if (isDispatchedFamily()) {
            return fullyPaid ? DISPATCHED_PAID : DISPATCHED_PARTIAL;
        }
        if (this == DELIVERED_UNPAID && fullyPaid) {
            return DELIVERED_PAID;
        }
        return this;
    }

    /** Allowed manual / system transitions. Same-status updates are always allowed (no-op). */
    public boolean canTransitionTo(OrderStatus next) {
        if (next == null) {
            return false;
        }
        if (this == next) {
            return true;
        }
        switch (this) {
            case AWAITING_DISPATCH:
                return next.isDispatchedFamily() || next == CANCELLED;
            case DISPATCHED_PAID:
            case DISPATCHED_PARTIAL:
            case DISPATCHED_UNPAID:
                return next.isDispatchedFamily()
                        || next == DELIVERED_PAID || next == DELIVERED_UNPAID
                        || next == RETURN_REQUESTED || next == CANCELLED;
            case DELIVERED_PAID:
            case DELIVERED_UNPAID:
                return next == DELIVERED_PAID || next == DELIVERED_UNPAID
                        || next == COMPLETED || next == RETURN_REQUESTED;
            case COMPLETED:
                return next == RETURN_REQUESTED;
            case RETURN_REQUESTED:
                return next == RETURN_IN_TRANSIT;
            case RETURN_IN_TRANSIT:
                return next == RETURNED_AWAITING_INSPECTION;
            case RETURNED_AWAITING_INSPECTION:
                return next == RETURNED_GOOD || next == RETURNED_DAMAGED;
            default:
                return false; // CANCELLED, RETURNED_GOOD, RETURNED_DAMAGED are terminal
        }
    }
}
