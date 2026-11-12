package com.devsam.urbantrend.Controller;

import com.devsam.urbantrend.Repository.LeadRepository;
import com.devsam.urbantrend.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.net.http.HttpResponse;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

class WebhookSecurityIntegrationTest extends IntegrationTestBase {

    private static final String WP_FORMS_SECRET = "test-wpforms-secret";
    private static final String KCB_SECRET = "test-kcb-secret";
    private static final AtomicLong ENTRY_IDS = new AtomicLong(900_000);

    @Autowired
    private LeadRepository leadRepository;

    @Test
    void wpFormsLeadWithoutSecretIsRejected() {
        HttpResponse<String> response = postJson("/api/v1/leads", leadPayload(ENTRY_IDS.incrementAndGet()));

        assertThat(response.statusCode()).isEqualTo(403);
    }

    @Test
    void wpFormsLeadWithWrongSecretIsRejected() {
        HttpResponse<String> response = postJson(
                "/api/v1/leads?secret=wrong-secret",
                leadPayload(ENTRY_IDS.incrementAndGet()));

        assertThat(response.statusCode()).isEqualTo(403);
    }

    @Test
    void wpFormsLeadWithValidSecretIsStored() {
        long entryId = ENTRY_IDS.incrementAndGet();

        HttpResponse<String> response = postJson(
                "/api/v1/leads?secret=" + WP_FORMS_SECRET,
                leadPayload(entryId));

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("Lead stored successfully");
        assertThat(leadRepository.existsByWpEntryId(entryId)).isTrue();
    }

    @Test
    void wpFormsRetriedSubmissionIsIgnored() {
        long entryId = ENTRY_IDS.incrementAndGet();
        String path = "/api/v1/leads?secret=" + WP_FORMS_SECRET;

        assertThat(postJson(path, leadPayload(entryId)).statusCode()).isEqualTo(200);

        HttpResponse<String> retry = postJson(path, leadPayload(entryId));

        assertThat(retry.statusCode()).isEqualTo(200);
        assertThat(retry.body()).contains("Duplicate lead ignored");
    }

    @Test
    void kcbIpnWithoutSecretIsRejected() {
        HttpResponse<String> response = postJson("/api/v1/webhooks/kcb/ipn", "{}");

        assertThat(response.statusCode()).isEqualTo(403);
    }

    @Test
    void kcbIpnWithWrongSecretIsRejected() {
        HttpResponse<String> response = postJson("/api/v1/webhooks/kcb/ipn?token=wrong", "{}");

        assertThat(response.statusCode()).isEqualTo(403);
    }

    @Test
    void kcbIpnWithSecretButIncompletePayloadIsRejected() {
        HttpResponse<String> response = postJson(
                "/api/v1/webhooks/kcb/ipn?token=" + KCB_SECRET,
                "{\"header\":{\"messageID\":\"m-1\"}}");

        assertThat(response.statusCode()).isEqualTo(400);
    }

    private String leadPayload(long entryId) {
        return """
                {
                  "formId": 7,
                  "formName": "Test Product",
                  "wpEntryId": %d,
                  "fields": {
                    "Name": "Webhook Tester",
                    "Phone Number": "0712345678",
                    "Location": "Nairobi"
                  }
                }
                """.formatted(entryId);
    }
}
