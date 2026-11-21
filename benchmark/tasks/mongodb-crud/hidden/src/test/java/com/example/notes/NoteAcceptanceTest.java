package com.example.notes;

import be.appify.prefab.test.IntegrationTest;
import benchmark.support.Api;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class NoteAcceptanceTest {
    @Autowired
    MockMvc mockMvc;

    @Test
    void notesCanBeCreatedArchivedAndDeleted() throws Exception {
        var api = new Api(mockMvc);
        var id = api.create("/notes", "{\"title\": \"Groceries\", \"text\": \"Milk\"}");

        api.get("/notes/{id}", id).andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Groceries"))
                .andExpect(jsonPath("$.text").value("Milk"))
                .andExpect(jsonPath("$.archived").value(false));

        api.postEmpty("/notes/{id}/archive", id).andExpect(status().isOk());
        api.get("/notes/{id}", id).andExpect(jsonPath("$.archived").value(true));

        api.delete("/notes/{id}", id).andExpect(status().isNoContent());
        api.get("/notes/{id}", id).andExpect(status().isNotFound());
    }

    @Test
    void listFiltersByTitleAndArchived() throws Exception {
        var api = new Api(mockMvc);
        var old = api.create("/notes", "{\"title\": \"Old groceries\", \"text\": \"Bread\"}");
        api.create("/notes", "{\"title\": \"Groceries\", \"text\": \"Milk\"}");
        api.create("/notes", "{\"title\": \"Ideas\", \"text\": \"Benchmark\"}");
        api.postEmpty("/notes/{id}/archive", old).andExpect(status().isOk());

        List<String> groceries = Api.read(api.get("/notes?title=GROCER&size=50"), "$.content[*].title");
        assertThat(groceries).containsExactlyInAnyOrder("Old groceries", "Groceries");
        List<String> active = Api.read(api.get("/notes?archived=false&size=50"), "$.content[*].title");
        assertThat(active).containsExactlyInAnyOrder("Groceries", "Ideas");
        List<String> archivedGroceries = Api.read(api.get("/notes?title=grocer&archived=true&size=50"), "$.content[*].title");
        assertThat(archivedGroceries).containsExactly("Old groceries");
    }
}
