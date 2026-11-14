package com.devsam.urbantrend.Controller;

import com.devsam.urbantrend.Entity.Requisition;
import com.devsam.urbantrend.Entity.User;
import com.devsam.urbantrend.Repository.UserRepository;
import com.devsam.urbantrend.Roles.UserRole;
import com.devsam.urbantrend.Service.Auth.RequisitionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RequisitionControllerTest {

    @Mock private RequisitionService requisitionService;
    @Mock private UserRepository userRepository;

    private RequisitionController controller;
    private final Pageable pageable = PageRequest.of(0, 10);
    private final Page<Requisition> empty = new PageImpl<>(List.<Requisition>of());

    @BeforeEach
    void setUp() {
        controller = new RequisitionController(requisitionService, userRepository);
    }

    private UserDetails signedInAs(UserRole role) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setRole(role);
        user.setEmail(role.name().toLowerCase() + "@example.com");
        lenient().when(userRepository.findByEmailAndDeletedAtIsNull(user.getEmail()))
                .thenReturn(Optional.of(user));
        lenient().when(requisitionService.getMine(user, pageable)).thenReturn(empty);
        lenient().when(requisitionService.getAll(any(), any())).thenReturn(empty);
        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail()).password("x").authorities("CAN_VIEW_REQUISITIONS").build();
    }

    @Test
    void staffAndCashiersOnlyGetTheirOwnRequisitions() {
        for (UserRole role : new UserRole[]{UserRole.STAFF, UserRole.CASHIER}) {
            controller.all(null, pageable, signedInAs(role));
        }

        verify(requisitionService, never()).getAll(any(), any());
    }

    @Test
    void adminsAndManagersGetTheFullList() {
        controller.all(null, pageable, signedInAs(UserRole.ADMIN));
        controller.all(null, pageable, signedInAs(UserRole.MANAGER));

        verify(requisitionService, org.mockito.Mockito.times(2)).getAll(any(), any());
    }
}
