package com.devsam.urbantrend.Dtos;

import com.devsam.urbantrend.Entity.Lead;
import com.devsam.urbantrend.Entity.User;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class LeadResponseTest {

    @Test
    void includesWhoTheLeadIsAssignedTo() {
        User staff = new User();
        staff.setId(UUID.randomUUID());
        staff.setFirstName("Jane");
        staff.setLastName("Doe");

        Lead lead = new Lead();
        lead.setId(9L);
        lead.setAssignedTo(staff);

        LeadResponse response = LeadResponse.from(lead);

        assertThat(response.getAssignedToId()).isEqualTo(staff.getId());
        assertThat(response.getAssignedToName()).isEqualTo("Jane Doe");
    }

    @Test
    void unassignedLeadHasNoAssignee() {
        Lead lead = new Lead();
        lead.setId(9L);

        LeadResponse response = LeadResponse.from(lead);

        assertThat(response.getAssignedToId()).isNull();
        assertThat(response.getAssignedToName()).isNull();
    }
}
