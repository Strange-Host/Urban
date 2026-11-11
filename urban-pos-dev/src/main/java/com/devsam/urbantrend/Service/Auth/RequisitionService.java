package com.devsam.urbantrend.Service.Auth;

import com.devsam.urbantrend.Dtos.RequisitionRequest;
import com.devsam.urbantrend.Dtos.RequisitionResponse;
import com.devsam.urbantrend.Entity.Requisition;
import com.devsam.urbantrend.Entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface RequisitionService {
    Requisition create(RequisitionRequest req, User user);

    Page<Requisition> getMine(User user, Pageable pageable);

    Page<Requisition> getAll(Pageable pageable);

    Requisition approve(UUID id, User approver);

    Requisition reject(UUID id, String reason, User approver);

    Requisition markPaid(UUID id, User payer);
}
