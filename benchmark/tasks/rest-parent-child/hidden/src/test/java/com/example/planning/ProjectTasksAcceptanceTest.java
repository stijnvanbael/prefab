package com.example.planning;

import be.appify.prefab.test.IntegrationTest;
import benchmark.support.Api;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class ProjectTasksAcceptanceTest {
    @Autowired
    MockMvc mockMvc;

    @Test
    void tasksAreListedPerProject() throws Exception {
        var api = new Api(mockMvc);
        var website = api.create("/projects", "{\"name\": \"Website\"}");
        var app = api.create("/projects", "{\"name\": \"App\"}");
        api.create("/projects/{projectId}/tasks", "{\"title\": \"Design\"}", website);
        api.create("/projects/{projectId}/tasks", "{\"title\": \"Build\"}", website);
        api.create("/projects/{projectId}/tasks", "{\"title\": \"Publish\"}", app);

        api.get("/projects/{projectId}/tasks", website)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[*].title", containsInAnyOrder("Design", "Build")));
    }

    @Test
    void taskCanBeCompleted() throws Exception {
        var api = new Api(mockMvc);
        var project = api.create("/projects", "{\"name\": \"Website\"}");
        var task = api.create("/projects/{projectId}/tasks", "{\"title\": \"Design\"}", project);

        api.get("/projects/{projectId}/tasks/{id}", project, task)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Design"))
                .andExpect(jsonPath("$.done").value(false));

        api.postEmpty("/projects/{projectId}/tasks/{id}/complete", project, task).andExpect(status().isOk());

        api.get("/projects/{projectId}/tasks/{id}", project, task).andExpect(jsonPath("$.done").value(true));
    }

    @Test
    void taskIsOnlyReachableThroughItsOwnProject() throws Exception {
        var api = new Api(mockMvc);
        var website = api.create("/projects", "{\"name\": \"Website\"}");
        var app = api.create("/projects", "{\"name\": \"App\"}");
        var task = api.create("/projects/{projectId}/tasks", "{\"title\": \"Design\"}", website);

        api.get("/projects/{projectId}/tasks/{id}", app, task).andExpect(status().isNotFound());
        api.postEmpty("/projects/{projectId}/tasks/{id}/complete", app, task).andExpect(status().isNotFound());

        api.get("/projects/{projectId}/tasks/{id}", website, task)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.done").value(false));
    }
}
