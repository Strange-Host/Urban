package com.devsam.urbantrend.Service.Auth.Messaging;

import com.devsam.urbantrend.Events.OrderCreatedEvent;
import com.devsam.urbantrend.Events.OrderPaidEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

@Component
@RequiredArgsConstructor
public class OrderMessagingListener {

    private final MessagingService messagingService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handleOrderPaid(OrderPaidEvent event) {
        messagingService.sendOrderPaidSms(event.orderId());
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handleOrderCreated(OrderCreatedEvent event) {
        messagingService.sendOrderCreatedSms(event.orderId());
    }
}
