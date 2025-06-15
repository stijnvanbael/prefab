package be.appify.prefab.example.todo;

import be.appify.prefab.example.IntegrationTest;
import be.appify.prefab.example.todo.infrastructure.persistence.TodoCrudRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.http.HttpHeaders.LOCATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class TodoIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TodoCrudRepository todoRepository;

    @BeforeEach
    void setup() {
        todoRepository.deleteAll();
    }

    @Test
    void getAllTodos() throws Exception {
        givenTodo("Foo");
        givenTodo("Bar");

        mockMvc.perform(get("/todos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)));
    }

    @Test
    void pagedAndSorted() throws Exception {
        givenTodo("Foo");
        givenTodo("Bar");

        mockMvc.perform(get("/todos?sort=description&page=1&size=1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].description").value("Foo"));
    }

    @Test
    void filterTodosByDescription() throws Exception {
        givenTodo("Foo");
        givenTodo("Bar");

        mockMvc.perform(get("/todos?description=Foo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].description").value("Foo"));
    }

    @Test
    void deleteById() throws Exception {
        var location = givenTodo("Foo");

        mockMvc.perform(get("/todos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)));

        mockMvc.perform(delete(location))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/todos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));

        mockMvc.perform(get(location))
                .andExpect(status().isNotFound());
    }

    @Test
    void createWithNullDescription() throws Exception {
        mockMvc.perform(post("/todos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":null}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateDescriptionToNull() throws Exception {
        var location = givenTodo("Foo");

        mockMvc.perform(put(location + "/description")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":null}"))
                .andExpect(status().isBadRequest());
    }

    private String givenTodo(String description) throws Exception {
        return mockMvc.perform(post("/todos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"%s\"}".formatted(description)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getHeader(LOCATION);
    }
}