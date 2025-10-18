package be.appify.prefab.example.todo;

import be.appify.prefab.example.IntegrationTest;
import be.appify.prefab.example.todo.application.CreateTodoRequest;
import be.appify.prefab.example.todo.application.TodoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
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
    private TodoRepository todoRepository;

    @Autowired
    TodoFixture todos;

    @BeforeEach
    void setup() {
        todoRepository.deleteAll();
    }

    @Test
    void getAllTodos() throws Exception {
        todos.givenTodoCreated(new CreateTodoRequest("Foo"));
        todos.givenTodoCreated(new CreateTodoRequest("Bar"));

        var result = todos.findTodos(PageRequest.of(0, 20), null);
        assertThat(result).hasSize(2);
    }

    @Test
    void pagedAndSorted() throws Exception {
        todos.givenTodoCreated(new CreateTodoRequest("Foo"));
        todos.givenTodoCreated(new CreateTodoRequest("Bar"));

        var result = todos.findTodos(PageRequest.of(1, 1, Sort.by("description")), null);
        assertThat(result)
                .hasSize(1)
                .allSatisfy(element -> assertThat(element.description()).isEqualTo("Foo"));
    }

    @Test
    void filterTodosByDescription() throws Exception {
        todos.givenTodoCreated(new CreateTodoRequest("Foo"));
        todos.givenTodoCreated(new CreateTodoRequest("Bar"));

        var result = todos.findTodos(PageRequest.of(0, 20), "Foo");
        assertThat(result)
                .hasSize(1)
                .allSatisfy(element -> assertThat(element.description()).isEqualTo("Foo"));
    }

    @Test
    void deleteById() throws Exception {
        var id = todos.givenTodoCreated(new CreateTodoRequest("Foo"));
        var location = "/todos/" + id;

        mockMvc.perform(get("/todos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)));

        todos.whenDeletingTodo(id);

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
        var id = todos.givenTodoCreated(new CreateTodoRequest("Foo"));
        var location = "/todos/" + id;

        mockMvc.perform(put(location + "/description")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":null}"))
                .andExpect(status().isBadRequest());
    }

}