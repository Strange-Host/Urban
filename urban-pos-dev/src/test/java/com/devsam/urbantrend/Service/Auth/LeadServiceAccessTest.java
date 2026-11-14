package com.devsam.urbantrend.Service.Auth;

import com.devsam.urbantrend.Entity.Lead;
import com.devsam.urbantrend.Entity.Product;
import com.devsam.urbantrend.Entity.User;
import com.devsam.urbantrend.Repository.LeadRepository;
import com.devsam.urbantrend.Repository.ProductRepository;
import com.devsam.urbantrend.Roles.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class LeadServiceAccessTest {

    @Mock private LeadRepository leadRepository;
    @Mock private ProductRepository productRepository;

    private LeadService service;
    private Lead lead;

    @BeforeEach
    void setUp() {
        service = new LeadService(leadRepository, productRepository);
        lead = new Lead();
        lead.setId(1L);
        lenient().when(leadRepository.findById(1L)).thenReturn(Optional.of(lead));
        lenient().when(productRepository.findAllByDeletedAtIsNullAndAssignedStaff(org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of());
    }

    private static User user(UserRole role) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setRole(role);
        return user;
    }

    @Test
    void staffCanWorkOnLeadsAssignedToThem() {
        User staff = user(UserRole.STAFF);
        lead.setAssignedTo(staff);

        assertThatCode(() -> service.requireAccess(1L, staff)).doesNotThrowAnyException();
    }

    @Test
    void staffCanWorkOnLeadsForTheirAssignedProducts() {
        User staff = user(UserRole.STAFF);
        lead.setFormName("Leather Wallet");
        Product wallet = Product.builder().name("leather wallet").build();
        lenient().when(productRepository.findAllByDeletedAtIsNullAndAssignedStaff(staff))
                .thenReturn(List.of(wallet));

        assertThatCode(() -> service.requireAccess(1L, staff)).doesNotThrowAnyException();
    }

    @Test
    void staffCannotTouchAnotherStaffMembersLead() {
        User staff = user(UserRole.STAFF);
        lead.setAssignedTo(user(UserRole.STAFF));
        lead.setFormName("Something else");

        assertThatThrownBy(() -> service.requireAccess(1L, staff))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void unassignedLeadIsNotOpenToEveryStaffMember() {
        assertThatThrownBy(() -> service.requireAccess(1L, user(UserRole.STAFF)))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void adminsAndManagersCanWorkOnAnyLead() {
        lead.setAssignedTo(user(UserRole.STAFF));

        assertThatCode(() -> service.requireAccess(1L, user(UserRole.ADMIN))).doesNotThrowAnyException();
        assertThatCode(() -> service.requireAccess(1L, user(UserRole.MANAGER))).doesNotThrowAnyException();
    }
}
