package com.example.helpdesk;

import be.appify.prefab.test.IntegrationTest;
import benchmark.support.Api;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class HelpdeskAcceptanceTest {
    @Autowired
    MockMvc mockMvc;

    private Api tenant(String tenant) {
        return new Api(mockMvc).with(request -> {
            request.addHeader("X-Tenant-Id", tenant);
            return request;
        });
    }

    private static String newTenant() {
        return "tenant-" + UUID.randomUUID();
    }

    private String ticket(Api api, String subject) throws Exception {
        return api.create("/tickets", "{\"subject\": \"%s\", \"description\": \"Every second page\"}".formatted(subject));
    }

    private String comment(Api api, String ticket, String text) throws Exception {
        return api.create("/tickets/{ticket}/comments", "{\"author\": \"Sam\", \"text\": \"%s\"}".formatted(text), ticket);
    }

    @Test
    void commentsMoveATicketForwardAndAreCounted() throws Exception {
        var acme = tenant(newTenant());
        var ticket = ticket(acme, "Printer jams");
        acme.get("/tickets/{id}", ticket).andExpect(status().isOk())
                .andExpect(jsonPath("$.subject").value("Printer jams"))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.commentCount").value(0));

        var first = comment(acme, ticket, "Looking into it");
        comment(acme, ticket, "New toner ordered");

        Api.eventually(Duration.ofSeconds(20), () -> acme.get("/tickets/{id}", ticket)
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.commentCount").value(2)));
        var comments = acme.get("/tickets/{id}/comments", ticket).andExpect(status().isOk());
        List<String> texts = Api.read(comments, "$.content[*].text");
        List<String> ids = Api.read(comments, "$.content[*].id");
        assertThat(texts).containsExactlyInAnyOrder("Looking into it", "New toner ordered");
        assertThat(ids).contains(first);

        acme.postEmpty("/tickets/{id}/resolution", ticket).andExpect(status().isOk());
        acme.get("/tickets/{id}", ticket).andExpect(jsonPath("$.status").value("RESOLVED"));
        comment(acme, ticket, "Works again");
        Api.eventually(Duration.ofSeconds(20), () -> acme.get("/tickets/{id}", ticket)
                .andExpect(jsonPath("$.commentCount").value(3)));
        acme.get("/tickets/{id}", ticket).andExpect(jsonPath("$.status").value("RESOLVED"));
    }

    @Test
    void ticketsAreListedByStatus() throws Exception {
        var acme = tenant(newTenant());
        var open = ticket(acme, "Open one");
        var resolved = ticket(acme, "Resolved one");
        acme.postEmpty("/tickets/{id}/resolution", resolved).andExpect(status().isOk());

        List<String> openIds = Api.read(acme.get("/tickets?status=OPEN").andExpect(status().isOk()), "$.content[*].id");
        List<String> resolvedIds = Api.read(acme.get("/tickets?status=RESOLVED"), "$.content[*].id");

        assertThat(openIds).containsExactly(open);
        assertThat(resolvedIds).containsExactly(resolved);
    }

    @Test
    void tenantsNeverSeeEachOthersTickets() throws Exception {
        var acme = tenant(newTenant());
        var globex = tenant(newTenant());
        var acmeTicket = ticket(acme, "Acme only");
        ticket(globex, "Globex only");

        globex.get("/tickets/{id}", acmeTicket).andExpect(status().isNotFound());
        List<String> globexOpen = Api.read(globex.get("/tickets?status=OPEN"), "$.content[*].subject");
        assertThat(globexOpen).containsExactly("Globex only");
        List<String> acmeOpen = Api.read(acme.get("/tickets?status=OPEN"), "$.content[*].subject");
        assertThat(acmeOpen).containsExactly("Acme only");
    }

    @Test
    void aTicketNeedsASubject() throws Exception {
        tenant(newTenant()).post("/tickets", "{\"subject\": \"\"}").andExpect(status().isBadRequest());
    }
}
