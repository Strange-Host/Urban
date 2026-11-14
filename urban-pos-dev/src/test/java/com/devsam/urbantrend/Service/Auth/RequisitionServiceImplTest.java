package com.devsam.urbantrend.Service.Auth;

import com.devsam.urbantrend.Entity.Requisition;
import com.devsam.urbantrend.Entity.User;
import com.devsam.urbantrend.Enum.RequisitionStatus;
import com.devsam.urbantrend.Repository.ExpenseRepository;
import com.devsam.urbantrend.Repository.RequisitionRepository;
import com.devsam.urbantrend.Repository.SalaryAdvanceRepository;
import com.devsam.urbantrend.Roles.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RequisitionServiceImplTest {

    @Mock private RequisitionRepository requisitionRepository;
    @Mock private ExpenseRepository expenseRepository;
    @Mock private SalaryAdvanceRepository salaryAdvanceRepository;

    private RequisitionServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new RequisitionServiceImpl(requisitionRepository, expenseRepository, salaryAdvanceRepository);
        lenient().when(requisitionRepository.save(any(Requisition.class)))
                .thenAnswer(call -> call.getArgument(0));
    }

    private static User user(UserRole role) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setRole(role);
        return user;
    }

    private Requisition pendingRequestFrom(User requester) {
        Requisition requisition = Requisition.builder()
                .requestedBy(requester)
                .status(RequisitionStatus.PENDING)
                .build();
        requisition.setId(UUID.randomUUID());
        lenient().when(requisitionRepository.findByIdAndDeletedAtIsNull(requisition.getId()))
                .thenReturn(Optional.of(requisition));
        return requisition;
    }

    @Test
    void managerCannotApproveTheirOwnRequisition() {
        User manager = user(UserRole.MANAGER);
        Requisition requisition = pendingRequestFrom(manager);

        assertThatThrownBy(() -> service.approve(requisition.getId(), manager))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("own requisition");

        verify(requisitionRepository, never()).save(any());
    }

    @Test
    void managerCannotApproveAnotherManagersRequisition() {
        Requisition requisition = pendingRequestFrom(user(UserRole.MANAGER));

        assertThatThrownBy(() -> service.approve(requisition.getId(), user(UserRole.MANAGER)))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Only an admin");
    }

    @Test
    void adminApprovesAManagersRequisitionAndTheTimeIsRecorded() {
        User admin = user(UserRole.ADMIN);
        Requisition requisition = pendingRequestFrom(user(UserRole.MANAGER));

        Requisition approved = service.approve(requisition.getId(), admin);

        assertThat(approved.getStatus()).isEqualTo(RequisitionStatus.APPROVED);
        assertThat(approved.getApprovedBy()).isSameAs(admin);
        assertThat(approved.getApprovedAt()).isNotNull();
    }

    @Test
    void managerStillApprovesStaffRequisitions() {
        User manager = user(UserRole.MANAGER);
        Requisition requisition = pendingRequestFrom(user(UserRole.STAFF));

        Requisition approved = service.approve(requisition.getId(), manager);

        assertThat(approved.getStatus()).isEqualTo(RequisitionStatus.APPROVED);
    }

    @Test
    void adminMayApproveTheirOwnRequisition() {
        User admin = user(UserRole.ADMIN);
        Requisition requisition = pendingRequestFrom(admin);

        assertThat(service.approve(requisition.getId(), admin).getStatus())
                .isEqualTo(RequisitionStatus.APPROVED);
    }

    @Test
    void staffCannotApproveTheirOwnRequisitionEvenWithThePermission() {
        User staff = user(UserRole.STAFF);
        Requisition requisition = pendingRequestFrom(staff);

        assertThatThrownBy(() -> service.approve(requisition.getId(), staff))
                .isInstanceOf(AccessDeniedException.class);
    }
}
