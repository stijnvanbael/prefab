package be.appify.prefab.example.todo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import be.appify.prefab.example.IntegrationTest;
import be.appify.prefab.example.todo.application.CreateTodoRequest;
import be.appify.prefab.example.todo.application.TodoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import static org.assertj.core.api.Assertions.assertThat;

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

        var result = todos.findTodos(PageRequest.of(0, 20), null);
        assertThat(result).hasSize(1);

        todos.whenDeletingTodo(id);

        result = todos.findTodos(PageRequest.of(0, 20), null);
        assertThat(result).hasSize(0);
    }

    @Test
    void createWithNullDescription() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/todos")
                        .with(SecurityMockMvcRequestPostProcessors.user("test"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":null}"))
                .andExpect(MockMvcResultMatchers.status().isBadRequest());
    }

    @Test
    void updateDescriptionToNull() throws Exception {
        var id = todos.givenTodoCreated(new CreateTodoRequest("Foo"));

        mockMvc.perform(MockMvcRequestBuilders.put("/todos/{id}/description", id)
                        .with(SecurityMockMvcRequestPostProcessors.user("test"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":null}"))
                .andExpect(MockMvcResultMatchers.status().isBadRequest());
    }

    @Test
    void unauthorized() throws Exception {
        var id = todos.givenTodoCreated(new CreateTodoRequest("Foo"));

        mockMvc.perform(MockMvcRequestBuilders.post("/todos/{id}/done", id)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isForbidden());
    }

    @Test
    void insufficientPermissions() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/todos")
                        .with(SecurityMockMvcRequestPostProcessors.user("test"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"Test todo\"}"))
                .andExpect(MockMvcResultMatchers.status().isForbidden());
    }

}