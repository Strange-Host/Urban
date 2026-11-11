package com.devsam.urbantrend.Service.Auth;

import com.devsam.urbantrend.Dtos.*;
import com.devsam.urbantrend.Entity.Courier;
import com.devsam.urbantrend.Entity.Customer;
import com.devsam.urbantrend.Entity.Lead;
import com.devsam.urbantrend.Entity.Product;
import com.devsam.urbantrend.Entity.User;
import com.devsam.urbantrend.Enum.OrderType;
import com.devsam.urbantrend.Events.OrderCreatedEvent;
import com.devsam.urbantrend.Repository.CourierRepository;
import com.devsam.urbantrend.Repository.CustomerRepository;
import com.devsam.urbantrend.Repository.LeadRepository;
import com.devsam.urbantrend.Repository.ProductRepository;
import com.devsam.urbantrend.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LeadConversionService {

    private final LeadRepository leadRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final OrderService orderService;
    private final ApplicationEventPublisher eventPublisher;
    private final UserRepository userRepository;
    private final CourierRepository courierRepository;

    @Transactional
    public void convertLead(Long leadId, ConvertLeadRequest request, User staffUser) {
        Lead lead = leadRepository.findById(leadId)
                .orElseThrow(() -> new RuntimeException("Lead not found"));

        if ("CONVERTED".equalsIgnoreCase(lead.getStatus())) {
            throw new RuntimeException("Lead already converted");
        }

        Customer customer = customerRepository.findByPhone(lead.getPhoneNumber())
                .orElseGet(() -> customerRepository.save(
                        Customer.builder()
                                .name(lead.getName())
                                .phone(lead.getPhoneNumber())
                                .city_town(lead.getLocation())
                                .build()
                ));

        List<OrderItemRequest> orderItems = new ArrayList<>();

        if (request.getItems() != null && !request.getItems().isEmpty()) {
            orderItems = request.getItems();
        } else {
            BigDecimal qty = (request.getQuantity() != null && request.getQuantity() > 0)
                    ? BigDecimal.valueOf(request.getQuantity())
                    : BigDecimal.ONE;

            orderItems = List.of(
                    new OrderItemRequest(request.getProductId(), qty)
            );
        }

        // validate chosen products
        for (OrderItemRequest item : orderItems) {
            if (item.getProductId() == null) {
                throw new RuntimeException("Each order item must include a productId");
            }

            productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new RuntimeException("Product not found: " + item.getProductId()));
        }

        String deliveryCity = (request.getDeliveryCity() != null && !request.getDeliveryCity().isBlank())
                ? request.getDeliveryCity()
                : lead.getLocation();

        BigDecimal deliveryFee = (request.getDeliveryFee() != null && request.getDeliveryFee().compareTo(BigDecimal.ZERO) >= 0)
                ? request.getDeliveryFee()
                : BigDecimal.ZERO;

        BigDecimal discount = (request.getDiscount() != null && request.getDiscount().compareTo(BigDecimal.ZERO) >= 0)
                ? request.getDiscount()
                : BigDecimal.ZERO;

        Courier courier = null;
        if (request.getCourierId() != null) {
            courier = courierRepository.findById(request.getCourierId())
                    .orElseThrow(() -> new RuntimeException("Courier not found: " + request.getCourierId()));

            if (!courier.isActive()) {
                throw new RuntimeException("Selected courier is inactive: " + courier.getName());
            }
        }

        OrderRequest orderRequest = OrderRequest.builder()
                .customerId(customer.getId())
                .orderType(OrderType.ONLINE)
                .items(orderItems)
                .deliveryCity(deliveryCity)
                .deliveryFee(deliveryFee)
                .discount(discount)
                .deliveryNotes("Lead Raw Data: " + lead.getRawData())
                .build();

        var order = orderService.create(orderRequest, staffUser);

        if (courier != null) {
            AssignCourierRequest assignCourierRequest = AssignCourierRequest.builder()
                    .courierId(courier.getId())
                    .build();

            orderService.assignCourier(order.getId(), assignCourierRequest, staffUser);
        }

        lead.setStatus("CONVERTED");
        lead.setConvertedOrderId(order.getId());
        leadRepository.save(lead);

        eventPublisher.publishEvent(new OrderCreatedEvent(order.getId()));
    }    public Page<LeadResponse> getAll(String status, Pageable pageable) {

        Page<Lead> leads = (status == null)
                ? leadRepository.findAll(pageable)
                : leadRepository.findByStatus(status, pageable);

        return leads.map(lead -> {

            Product product = productRepository
                    .findByNameIgnoreCase(lead.getFormName())
                    .orElse(null);

            return LeadResponse.builder()
                    .id(lead.getId())
                    .name(lead.getName())
                    .phoneNumber(lead.getPhoneNumber())
                    .location(lead.getLocation())
                    .status(lead.getStatus())
                    .created(OffsetDateTime.from(lead.getCreated()))
                    .productName(product != null ? product.getName() : null)
                    .sellingPrice(product != null ? String.valueOf(product.getSellingPrice()) : null)
                    .build();
        });
    }

    /**
     * Manually assign a lead to a specific staff member.
     * Replaces the old round-robin @Scheduled auto-assignment.
     */
    @Transactional
    public void assignLeadToStaff(Long id, UUID staffId) {

        final Logger log = LoggerFactory.getLogger(LeadConversionService.class);

        Lead lead = leadRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Lead not found"));

        User staff = userRepository.findById(staffId)
                .orElseThrow(() -> new RuntimeException("Staff not found"));

        lead.setAssignedTo(staff);
        leadRepository.save(lead);

        log.info("👤 Lead {} manually assigned to staff {} ({})",
                id, staff.getId(), staff.getEmail());
    }
}