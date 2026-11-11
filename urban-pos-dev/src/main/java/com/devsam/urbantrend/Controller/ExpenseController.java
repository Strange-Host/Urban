package com.devsam.urbantrend.Controller;

import com.devsam.urbantrend.Dtos.ExpenseRequest;
import com.devsam.urbantrend.Entity.Expense;
import com.devsam.urbantrend.Entity.User;
import com.devsam.urbantrend.Errors.ResourceNotFoundException;
import com.devsam.urbantrend.Repository.ExpenseRepository;
import com.devsam.urbantrend.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/expenses")
@RequiredArgsConstructor
public class ExpenseController {

    private final ExpenseRepository expenseRepository;
    private final UserRepository userRepository;

    private User me(UserDetails ud) {
        return userRepository.findByEmail(ud.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('CAN_CREATE_EXPENSES')")
    public Expense create(
            @RequestBody ExpenseRequest req,
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

        return expenseRepository.save(expense);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('CAN_VIEW_EXPENSES')")
    public Page<Expense> all(Pageable pageable) {
        return expenseRepository.findAllByDeletedAtIsNull(pageable);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('CAN_DELETE_EXPENSES')")
    public void softDelete(@PathVariable UUID id) {
        Expense e = expenseRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found"));
        expenseRepository.save(e);
    }
}