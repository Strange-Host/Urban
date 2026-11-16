package com.devsam.urbantrend.Security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ActivityLogClassifierTest {

    private static final String ID = "3f2b8c1e-7a4d-4e1b-9c55-0a1b2c3d4e5f";

    private static ActivityLogClassifier.Entry classify(String method, String path) {
        return ActivityLogClassifier.classify(method, path, null);
    }

    @Test
    void readsAreNeverLogged() {
        assertThat(classify("GET", "/api/v1/orders")).isNull();
        assertThat(classify("HEAD", "/api/v1/orders")).isNull();
        assertThat(classify("OPTIONS", "/api/v1/orders")).isNull();
    }

    @Test
    void creatingThingsIsLoggedAsCreate() {
        assertThat(classify("POST", "/api/v1/orders").action()).isEqualTo("CREATE");
        assertThat(classify("POST", "/api/v1/orders").entity()).isEqualTo("ORDER");
        assertThat(classify("POST", "/api/v1/payments").entity()).isEqualTo("PAYMENT");
        assertThat(classify("POST", "/api/v1/products").entity()).isEqualTo("PRODUCT");
        assertThat(classify("POST", "/api/v1/auth/register"))
                .extracting(ActivityLogClassifier.Entry::action, ActivityLogClassifier.Entry::entity)
                .containsExactly("CREATE", "USER");
    }

    @Test
    void actionEndpointsAreUpdatesOfAnExistingThing() {
        assertThat(classify("POST", "/api/v1/inventory/restock").action()).isEqualTo("UPDATE");
        assertThat(classify("POST", "/api/v1/payments/" + ID + "/match/" + ID).action()).isEqualTo("UPDATE");
        assertThat(classify("PATCH", "/api/v1/requisitions/" + ID + "/approve").action()).isEqualTo("UPDATE");
        assertThat(classify("PUT", "/api/v1/products/" + ID).action()).isEqualTo("UPDATE");
    }

    @Test
    void statusChangesKeepTheirQueryInTheDescription() {
        ActivityLogClassifier.Entry entry =
                ActivityLogClassifier.classify("PATCH", "/api/v1/orders/" + ID + "/status", "status=CANCELLED");

        assertThat(entry.entity()).isEqualTo("ORDER");
        assertThat(entry.entityId()).isEqualTo(ID);
        assertThat(entry.description()).isEqualTo("PATCH /api/v1/orders/" + ID + "/status?status=CANCELLED");
    }

    @Test
    void deletesAreLoggedSoTheDeletesFigureIsNotAlwaysZero() {
        assertThat(classify("DELETE", "/api/v1/customers/" + ID))
                .extracting(ActivityLogClassifier.Entry::action, ActivityLogClassifier.Entry::entity)
                .containsExactly("DELETE", "CUSTOMER");
        assertThat(classify("DELETE", "/api/v1/category/" + ID).entity()).isEqualTo("CATEGORY");
        assertThat(classify("DELETE", "/api/v1/auth/user/" + ID).entity()).isEqualTo("USER");
    }

    @Test
    void permissionChangesAreLogged() {
        ActivityLogClassifier.Entry entry = classify("PUT", "/api/v1/admin/permissions/" + ID);

        assertThat(entry.entity()).isEqualTo("PERMISSIONS");
        assertThat(entry.entityId()).isEqualTo(ID);
    }

    @Test
    void scansAreKeptApartFromOrderEdits() {
        assertThat(classify("POST", "/api/v1/orders/scan").entity()).isEqualTo("DISPATCH");
    }

    @Test
    void signInTokenWebhookAndLeadTrafficIsNotActivity() {
        assertThat(classify("POST", "/api/v1/auth/login")).isNull();
        assertThat(classify("POST", "/api/v1/auth/verify-otp")).isNull();
        assertThat(classify("POST", "/api/v1/auth/refresh")).isNull();
        assertThat(classify("POST", "/api/v1/auth/logout")).isNull();
        assertThat(classify("POST", "/api/v1/webhooks/kcb/ipn")).isNull();
        assertThat(classify("POST", "/api/v1/shopify/webhook")).isNull();
        assertThat(classify("PATCH", "/api/v1/notifications/read-all")).isNull();
        // Leads already write their own, richer log entries.
        assertThat(classify("PATCH", "/api/v1/leads/5/status")).isNull();
        assertThat(classify("POST", "/not-the-api")).isNull();
    }
}
