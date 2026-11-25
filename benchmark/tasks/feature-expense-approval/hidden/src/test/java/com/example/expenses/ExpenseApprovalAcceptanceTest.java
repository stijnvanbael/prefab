package com.example.expenses;

import be.appify.prefab.test.IntegrationTest;
import benchmark.support.Api;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class ExpenseApprovalAcceptanceTest {
    private static final String CLAIM = "{\"description\": \"Train to Brussels\", \"amount\": 42.50}";

    @Autowired
    MockMvc mockMvc;

    private Api as(String name, String role) {
        return new Api(mockMvc).with(user(name).roles(role));
    }

    private Api erin() {
        return as("erin", "EMPLOYEE");
    }

    private Api manager() {
        return as("mia", "MANAGER");
    }

    private Api finance() {
        return as("finn", "FINANCE");
    }

    private List<String> reimbursementsOf(String claim) throws Exception {
        return Api.read(finance().get("/reimbursements?claim={claim}", claim).andExpect(status().isOk()),
                "$.content[*].id");
    }

    @Test
    void anApprovedClaimIsReimbursedByFinance() throws Exception {
        var claim = erin().create("/expense-claims", CLAIM);
        var submitted = erin().get("/expense-claims/{id}", claim).andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Train to Brussels"))
                .andExpect(jsonPath("$.status").value("SUBMITTED"))
                .andExpect(jsonPath("$.submittedBy").value("erin"));
        assertThat(Api.number(submitted, "$.amount")).isCloseTo(42.5, within(0.001));

        manager().postEmpty("/expense-claims/{id}/approval", claim).andExpect(status().isOk());
        manager().postEmpty("/expense-claims/{id}/approval", claim).andExpect(status().isOk());
        erin().get("/expense-claims/{id}", claim).andExpect(jsonPath("$.status").value("APPROVED"));

        Api.eventually(Duration.ofSeconds(20), () -> assertThat(reimbursementsOf(claim)).hasSize(1));
        var reimbursement = reimbursementsOf(claim).getFirst();
        var pending = finance().get("/reimbursements/{id}", reimbursement).andExpect(status().isOk())
                .andExpect(jsonPath("$.employee").value("erin"))
                .andExpect(jsonPath("$.status").value("PENDING"));
        assertThat(Api.number(pending, "$.amount")).isCloseTo(42.5, within(0.001));

        finance().postEmpty("/reimbursements/{id}/payment", reimbursement).andExpect(status().isOk());
        finance().get("/reimbursements/{id}", reimbursement).andExpect(jsonPath("$.status").value("PAID"));
        Api.eventually(Duration.ofSeconds(20), () -> erin().get("/expense-claims/{id}", claim)
                .andExpect(jsonPath("$.status").value("REIMBURSED")));
        Thread.sleep(500);
        assertThat(reimbursementsOf(claim)).hasSize(1);
    }

    @Test
    void aRejectedClaimIsNeverReimbursed() throws Exception {
        var claim = erin().create("/expense-claims", CLAIM);

        manager().postEmpty("/expense-claims/{id}/rejection", claim).andExpect(status().isOk());
        manager().postEmpty("/expense-claims/{id}/approval", claim).andExpect(status().isOk());

        erin().get("/expense-claims/{id}", claim).andExpect(jsonPath("$.status").value("REJECTED"));
        Thread.sleep(1000);
        assertThat(reimbursementsOf(claim)).isEmpty();
    }

    @Test
    void everyStepNeedsItsRole() throws Exception {
        manager().post("/expense-claims", CLAIM).andExpect(status().isForbidden());
        new Api(mockMvc).post("/expense-claims", CLAIM).andExpect(status().isUnauthorized());
        erin().post("/expense-claims", "{\"description\": \"Free lunch\", \"amount\": 0}")
                .andExpect(status().isBadRequest());
        var claim = erin().create("/expense-claims", CLAIM);

        erin().postEmpty("/expense-claims/{id}/approval", claim).andExpect(status().isForbidden());
        finance().postEmpty("/expense-claims/{id}/rejection", claim).andExpect(status().isForbidden());
        manager().postEmpty("/expense-claims/{id}/approval", claim).andExpect(status().isOk());
        Api.eventually(Duration.ofSeconds(20), () -> assertThat(reimbursementsOf(claim)).hasSize(1));
        var reimbursement = reimbursementsOf(claim).getFirst();

        manager().get("/reimbursements?claim={claim}", claim).andExpect(status().isForbidden());
        manager().postEmpty("/reimbursements/{id}/payment", reimbursement).andExpect(status().isForbidden());
        erin().get("/reimbursements/{id}", reimbursement).andExpect(status().isForbidden());
    }
}
