package com.devsam.urbantrend.Security;

import com.devsam.urbantrend.Entity.Order;
import com.devsam.urbantrend.Entity.User;
import com.devsam.urbantrend.Roles.UserRole;
import org.springframework.security.access.AccessDeniedException;

/**
 * Who may see and change which orders. Staff only work with their own orders (ones they
 * created, or that were assigned to them, e.g. from one of their leads); admins, managers
 * and cashiers see everything because they dispatch, collect payment and oversee.
 */
public final class OrderAccess {

    private OrderAccess() {
    }

    public static boolean seesAllOrders(User user) {
        return user.getRole() != UserRole.STAFF;
    }

    public static boolean owns(User user, Order order) {
        return user.getId() != null
                && (user.getId().equals(order.getCreatedBy()) || user.getId().equals(order.getAssignedTo()));
    }

    public static void requireAccess(User user, Order order) {
        if (!seesAllOrders(user) && !owns(user, order)) {
            throw new AccessDeniedException("You can only access your own orders");
        }
    }
}
