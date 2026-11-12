package com.devsam.urbantrend.Service.Auth;
import com.devsam.urbantrend.Dtos.PaymentRequest;
import com.devsam.urbantrend.Dtos.PaymentResponse;
import com.devsam.urbantrend.Entity.User;
import com.devsam.urbantrend.Enum.PaymentMethod;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface PaymentService {

    PaymentResponse recordPayment(PaymentRequest request, User user);

    Page<PaymentResponse> getAllPayments(Pageable pageable, String query, PaymentMethod method);

    List<PaymentResponse> getPaymentsForOrder(UUID orderId);

    Page<PaymentResponse> getUnmatchedPayments(Pageable pageable);

    PaymentResponse matchPaymentToOrder(UUID paymentId, UUID orderId, User user);
}
