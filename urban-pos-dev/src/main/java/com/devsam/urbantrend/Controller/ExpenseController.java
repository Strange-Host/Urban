package com.devsam.urbantrend.Controller;

import com.devsam.urbantrend.Dtos.ExpenseRequest;
import com.devsam.urbantrend.Dtos.ExpenseResponse;
import com.devsam.urbantrend.Entity.Expense;
import com.devsam.urbantrend.Entity.User;
import com.devsam.urbantrend.Errors.ResourceNotFoundException;
import com.devsam.urbantrend.Repository.ExpenseRepository;
import com.devsam.urbantrend.Repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/expenses")
@RequiredArgsConstructor
public class ExpenseController {

    private final ExpenseRepository expenseRepository;
    private final UserRepository userRepository;

    private User me(UserDetails ud) {
        return userRepository.findByEmailAndDeletedAtIsNull(ud.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('CAN_CREATE_EXPENSES')")
    public ExpenseResponse create(
            @Valid @RequestBody ExpenseRequest req,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Expense expense = Expense.builder()
                .expenseType(req.getExpenseType())
                .amount(req.getAmount())
                .description(req.getDescription())
                .paymentMethod(req.getPaymentMethod())
                .referenceCode(req.getReferenceCode())
                .createdBy(me(userDetails))
                .build();

        return ExpenseResponse.from(expenseRepository.save(expense));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('CAN_VIEW_EXPENSES')")
    public Page<ExpenseResponse> all(Pageable pageable) {
        return expenseRepository.findAllByDeletedAtIsNull(pageable)
                .map(ExpenseResponse::from);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('CAN_DELETE_EXPENSES')")
    public void softDelete(@PathVariable UUID id) {
        Expense expense = expenseRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found"));

        expense.setDeletedAt(LocalDateTime.now());
        expenseRepository.save(expense);
    }
}