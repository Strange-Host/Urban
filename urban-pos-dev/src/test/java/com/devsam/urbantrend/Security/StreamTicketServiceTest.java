package com.devsam.urbantrend.Security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StreamTicketServiceTest {

    private final StreamTicketService service = new StreamTicketService();

    @Test
    void aTicketWorksOnceForItsOwner() {
        String ticket = service.issue("ann@example.com");

        assertThat(service.redeem(ticket)).contains("ann@example.com");
        assertThat(service.redeem(ticket)).isEmpty();
    }

    @Test
    void ticketsAreUnguessableAndDistinct() {
        String first = service.issue("ann@example.com");
        String second = service.issue("ann@example.com");

        assertThat(first).isNotEqualTo(second).hasSizeGreaterThanOrEqualTo(40);
    }

    @Test
    void unknownBlankAndNullTicketsAreRejected() {
        assertThat(service.redeem("not-a-ticket")).isEmpty();
        assertThat(service.redeem("  ")).isEmpty();
        assertThat(service.redeem(null)).isEmpty();
    }
}
