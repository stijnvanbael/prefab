package com.example.planning;

import be.appify.prefab.test.IntegrationTest;
import benchmark.support.Api;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class TenantIsolationAcceptanceTest {
    @Autowired
    MockMvc mockMvc;

    private Api tenant(String tenant) {
        return new Api(mockMvc).with(request -> {
            request.addHeader("X-Tenant-Id", tenant);
            return request;
        });
    }

    @Test
    void tenantsOnlySeeTheirOwnProjects() throws Exception {
        var acme = tenant("acme");
        var globex = tenant("globex");
        var rocket = acme.create("/projects", "{\"name\": \"Rocket\"}");
        acme.create("/projects", "{\"name\": \"Anvil\"}");
        var merger = globex.create("/projects", "{\"name\": \"Merger\"}");

        acme.get("/projects/{id}", rocket).andExpect(status().isOk());
        acme.get("/projects/{id}", merger).andExpect(status().isNotFound());
        globex.get("/projects/{id}", rocket).andExpect(status().isNotFound());

        List<String> acmeNames = Api.read(acme.get("/projects?size=50"), "$.content[*].name");
        assertThat(acmeNames).containsExactlyInAnyOrder("Rocket", "Anvil");
        List<String> globexNames = Api.read(globex.get("/projects?size=50"), "$.content[*].name");
        assertThat(globexNames).containsExactly("Merger");
    }

    @Test
    void tenantInTheBodyIsIgnored() throws Exception {
        var id = tenant("acme").create("/projects", "{\"name\": \"Sneaky\", \"tenant\": \"globex\", \"tenantId\": \"globex\"}");

        tenant("acme").get("/projects/{id}", id).andExpect(status().isOk());
        tenant("globex").get("/projects/{id}", id).andExpect(status().isNotFound());
    }
}
