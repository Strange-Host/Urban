package com.devsam.urbantrend.Controller;

import com.devsam.urbantrend.Entity.User;
import com.devsam.urbantrend.Enum.Permission;
import com.devsam.urbantrend.Roles.UserRole;
import com.devsam.urbantrend.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;

import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

class AuthorizationIntegrationTest extends IntegrationTestBase {

    @Test
    void ordersAreUnauthorizedWithoutToken() {
        assertThat(get("/api/v1/orders", null).statusCode()).isEqualTo(401);
    }

    @Test
    void ordersAreForbiddenWithoutTheViewPermission() {
        User cashier = createUser(UserRole.CASHIER);

        assertThat(get("/api/v1/orders", bearer(cashier)).statusCode()).isEqualTo(403);
    }

    @Test
    void ordersAreReturnedWithTheViewPermission() {
        User manager = createUser(UserRole.MANAGER, Permission.CAN_VIEW_ORDERS);

        HttpResponse<String> response = get("/api/v1/orders", bearer(manager));

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"totalElements\"");
    }

    @Test
    void requestedPageSizeIsCappedByConfiguration() {
        User admin = createUser(UserRole.ADMIN);

        HttpResponse<String> response = get("/api/v1/orders?page=0&size=1000", bearer(admin));

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"size\":500");
    }

    @Test
    void unknownOrderStatusIsRejectedAsBadRequest() {
        User admin = createUser(UserRole.ADMIN);

        assertThat(get("/api/v1/orders?status=NOT_A_STATUS", bearer(admin)).statusCode())
                .isEqualTo(400);
    }

    @Test
    void profitReportRequiresItsPermission() {
        User manager = createUser(UserRole.MANAGER, Permission.CAN_VIEW_ORDERS);

        HttpResponse<String> response = get(
                "/api/reports/profit?from=2026-01-01T00:00:00&to=2026-01-31T23:59:59",
                bearer(manager));

        assertThat(response.statusCode()).isEqualTo(403);
    }

    @Test
    void adminCanReadTheProfitReport() {
        User admin = createUser(UserRole.ADMIN);

        HttpResponse<String> response = get(
                "/api/reports/profit?from=2026-01-01T00:00:00&to=2026-01-31T23:59:59",
                bearer(admin));

        assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
        assertThat(response.body()).contains("grossProfit").contains("netProfit");
    }

    @Test
    void staffDirectoryRequiresAnElevatedRole() {
        User cashier = createUser(UserRole.CASHIER, Permission.CAN_VIEW_ORDERS);
        User manager = createUser(UserRole.MANAGER, Permission.CAN_VIEW_ORDERS);

        assertThat(get("/api/v1/auth/staffs", bearer(cashier)).statusCode()).isEqualTo(403);
        assertThat(get("/api/v1/auth/staffs", bearer(manager)).statusCode()).isEqualTo(200);
    }

    @Test
    void notificationsAreScopedToTheAuthenticatedUser() {
        User cashier = createUser(UserRole.CASHIER);

        assertThat(get("/api/v1/notifications/unread", null).statusCode()).isEqualTo(401);
        assertThat(get("/api/v1/notifications/unread", bearer(cashier)).statusCode()).isEqualTo(200);
    }
}
