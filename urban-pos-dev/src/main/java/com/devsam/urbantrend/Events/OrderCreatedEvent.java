package com.devsam.urbantrend.Events;

import lombok.Data;

import java.util.UUID;

public record OrderCreatedEvent(
        UUID orderId){
}
