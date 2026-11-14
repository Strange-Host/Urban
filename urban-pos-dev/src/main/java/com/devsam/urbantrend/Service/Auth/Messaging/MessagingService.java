package com.devsam.urbantrend.Service.Auth.Messaging;
import com.devsam.urbantrend.Entity.*;
import com.devsam.urbantrend.Repository.CustomerRepository;
import com.devsam.urbantrend.Repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static java.util.Arrays.stream;

@Service
@RequiredArgsConstructor
public class MessagingService {

    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final SmsProvider smsProvider;
    private static final Logger log =
            LoggerFactory.getLogger(MessagingService.class);

    public void sendOrderPaidSms(UUID orderId) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        Customer customer = customerRepository.findById(order.getCustomerId())
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        String phone = normalizePhone(customer.getPhone());

        if (phone == null) {
            log.warn("Paid SMS skipped for order {}: customer has no phone number", orderId);
            return;
        }

        String message = """
                URBAN TRENDS ✅
                Hi %s, we have received your payment of KES %s.
                
                here is our catalog: https://urbantrends1.com/collections/all
                Thank you for shopping with us.
                """.formatted(customer.getName(), order.getPaidAmount());

        smsProvider.sendSms(phone, message);
    }


//    send converted message

    public void sendOrderCreatedSms(UUID orderId) {

        Order order = orderRepository.findByIdWithItemsAndProducts(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        Optional<String> firstProductName = order.getItems().stream()
                .map(OrderItem::getProduct)
                .filter(Objects::nonNull)
                .map(Product::getName)
                .findFirst();

        String productName = firstProductName.orElse("Unknown");

        Customer customer = customerRepository.findById(order.getCustomerId())
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        String phone = normalizePhone(customer.getPhone());

        if (phone == null) {
            log.warn("Order-created SMS skipped for order {}: customer has no phone number", orderId);
            return;
        }

        String message = """
        URBAN TRENDS 
        Hi %s, we have received your order for %s and processed it successfully.
        It will be delivered within the next 24 hours.
        
        
        order payments are: 
        paybill:    522533
        Account number: %s
        Thank you for choosing us.
        """.formatted(customer.getName(), productName, order.getAccountNumber());

        log.info("📲 Sending ORDER CREATED SMS | Product: {} | Account: {} | Phone: {} | Message: {}",
                productName,
                order.getAccountNumber(),
                phone,
                message.replaceAll("\\s+", " ").trim()
        );

        smsProvider.sendSms(phone, message);
    }

    public void sendOtpSms(String phone, String otp) {

        String normalizedPhone = normalizePhone(phone);

        if (normalizedPhone == null || normalizedPhone.isBlank()) {
            log.warn("Cannot send OTP SMS: no valid phone number");
            return;
        }

        String message = """
            URBAN TRENDS 🔐
            Your login OTP is: %s

            This code expires in 5 minutes.
            Do not share this code with anyone.
            """.formatted(otp);

        log.info("📲 Sending OTP SMS | Phone: {}", maskPhone(normalizedPhone));

        smsProvider.sendSms(normalizedPhone, message);
    }

    private String maskPhone(String phone) {
        if (phone.length() <= 4) {
            return "****";
        }
        return "****" + phone.substring(phone.length() - 4);
    }

    private String normalizePhone(String phone) {
        if (phone == null) return null;

        // Strip spaces, dashes, dots and brackets typed by staff ("0712 345 678").
        phone = phone.trim().replaceAll("[\\s\\-().]", "");
        if (phone.isEmpty()) return null;

        if (phone.startsWith("0")) {
            return "254" + phone.substring(1);
        }
        if (phone.startsWith("+")) {
            return phone.substring(1);
        }
        return phone;
    }
}

