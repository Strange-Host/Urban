package com.devsam.urbantrend.Service.Auth;

import com.devsam.urbantrend.Dtos.RequisitionRequest;
import com.devsam.urbantrend.Dtos.RequisitionResponse;
import com.devsam.urbantrend.Entity.Expense;
import com.devsam.urbantrend.Entity.Requisition;
import com.devsam.urbantrend.Entity.SalaryAdvance;
import com.devsam.urbantrend.Entity.User;
import com.devsam.urbantrend.Enum.ExpenseType;
import com.devsam.urbantrend.Enum.PaymentMethod;
import com.devsam.urbantrend.Enum.RequisitionStatus;
import com.devsam.urbantrend.Enum.RequisitionType;
import com.devsam.urbantrend.Errors.ResourceNotFoundException;
import com.devsam.urbantrend.Repository.ExpenseRepository;
import com.devsam.urbantrend.Repository.RequisitionRepository;
import com.devsam.urbantrend.Repository.SalaryAdvanceRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class RequisitionServiceImpl implements RequisitionService {

    private final RequisitionRepository requisitionRepository;
    private final ExpenseRepository expenseRepository;
    private final SalaryAdvanceRepository salaryAdvanceRepository;

    @Override
    public Requisition create(RequisitionRequest req, User user) {
        Requisition r = Requisition.builder()
                .requisitionType(req.getRequisitionType())
                .status(RequisitionStatus.PENDING)
                .title(req.getTitle())
                .description(req.getDescription())
                .amountRequested(req.getAmountRequested())
                .requestedBy(user)
                .build();
        return requisitionRepository.save(r);
    }

    @Override
    public Page<Requisition> getMine(User user, Pageable pageable) {
        return requisitionRepository
                .findAllByRequestedBy_IdAndDeletedAtIsNull(user.getId(), pageable);
    }

    @Override
    public Page<Requisition> getAll(RequisitionStatus status, Pageable pageable) {
        if (status == null) {
            return requisitionRepository.findAllByDeletedAtIsNull(pageable);
        }
        return requisitionRepository.findAllByStatusAndDeletedAtIsNull(status, pageable);
    }

    @Override
    public Requisition approve(UUID id, User approver) {
        Requisition r = requisitionRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Requisition not found"));

        if (r.getStatus() != RequisitionStatus.PENDING) {
            throw new IllegalStateException("Only PENDING requisitions can be approved");
        }

        r.setStatus(RequisitionStatus.APPROVED);
        r.setApprovedBy(approver);

        return requisitionRepository.save(r);
    }

    @Override
    public Requisition reject(UUID id, String reason, User approver) {
        Requisition r = requisitionRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Requisition not found"));

        if (r.getStatus() != RequisitionStatus.PENDING) {
            throw new IllegalStateException("Only PENDING requisitions can be rejected");
        }

        r.setStatus(RequisitionStatus.REJECTED);
        r.setRejectedBy(approver);
        r.setRejectedAt(OffsetDateTime.now());
        r.setRejectionReason(reason);

        return requisitionRepository.save(r);
    }

    @Override
    public Requisition markPaid(UUID id, User payer) {
        Requisition r = requisitionRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Requisition not found"));

        if (r.getStatus() != RequisitionStatus.APPROVED) {
            throw new IllegalStateException("Only APPROVED requisitions can be marked PAID");
        }

        r.setStatus(RequisitionStatus.PAID);
        r.setPaidBy(payer);
        r.setPaidAt(OffsetDateTime.now());

        if (r.getRequisitionType() == RequisitionType.SHOP_OPERATIONAL) {
            if (!expenseRepository.existsByRequisition_Id(r.getId())) {
                Expense expense = Expense.builder()
                        .expenseType(ExpenseType.MISC) // You can map better later
                        .amount(r.getAmountRequested())
                        .description("Auto-posted from requisition: " + r.getTitle())
                        .paymentMethod(PaymentMethod.CASH) // set real method later
                        .createdBy(payer)
                        .requisition(r)
                        .build();
                expenseRepository.save(expense);
            }
        } else if (r.getRequisitionType() == RequisitionType.SALARY_ADVANCE) {
            if (!salaryAdvanceRepository.existsByRequisition_Id(r.getId())) {
                SalaryAdvance adv = SalaryAdvance.builder()
                        .requisition(r)
                        .user(r.getRequestedBy())
                        .amount(r.getAmountRequested())
                        .note("Salary advance from requisition: " + r.getTitle())
                        .build();
                salaryAdvanceRepository.save(adv);
            }
        }

        return requisitionRepository.save(r);
    }
}
