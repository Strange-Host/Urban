package com.devsam.urbantrend.Security;

import com.devsam.urbantrend.Entity.Order;
import com.devsam.urbantrend.Entity.User;
import com.devsam.urbantrend.Roles.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderAccessTest {

    private static User user(UserRole role) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setRole(role);
        return user;
    }

    private static Order order(UUID createdBy, UUID assignedTo) {
        Order order = new Order();
        order.setCreatedBy(createdBy);
        order.setAssignedTo(assignedTo);
        return order;
    }

    @Test
    void staffSeeOnlyOrdersTheyCreated() {
        User staff = user(UserRole.STAFF);

        assertThat(OrderAccess.seesAllOrders(staff)).isFalse();
        assertThatCode(() -> OrderAccess.requireAccess(staff, order(staff.getId(), null)))
                .doesNotThrowAnyException();
    }

    @Test
    void staffAlsoSeeOrdersAssignedToThemEvenWhenSomeoneElseCreatedThem() {
        User staff = user(UserRole.STAFF);

        assertThatCode(() -> OrderAccess.requireAccess(staff, order(UUID.randomUUID(), staff.getId())))
                .doesNotThrowAnyException();
    }

    @Test
    void staffCannotTouchSomeoneElsesOrder() {
        User staff = user(UserRole.STAFF);

        assertThatThrownBy(() -> OrderAccess.requireAccess(staff, order(UUID.randomUUID(), null)))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> OrderAccess.requireAccess(staff, order(null, null)))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void adminManagerAndCashierSeeEveryOrder() {
        Order someonesOrder = order(UUID.randomUUID(), null);

        for (UserRole role : new UserRole[]{UserRole.ADMIN, UserRole.MANAGER, UserRole.CASHIER}) {
            User user = user(role);
            assertThat(OrderAccess.seesAllOrders(user)).isTrue();
            assertThatCode(() -> OrderAccess.requireAccess(user, someonesOrder)).doesNotThrowAnyException();
        }
    }
}
