package com.example.school;

import be.appify.prefab.test.IntegrationTest;
import benchmark.support.Api;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class CourseEnrollmentAcceptanceTest {
    @Autowired
    MockMvc mockMvc;

    private Api api() {
        return new Api(mockMvc);
    }

    private String course(String title) throws Exception {
        return api().create("/courses", "{\"title\": \"%s\", \"capacity\": 12}".formatted(title));
    }

    private String student(String name) throws Exception {
        return api().create("/students", "{\"name\": \"%s\", \"email\": \"%s@example.com\"}".formatted(name, name));
    }

    private String enroll(String course, String student) throws Exception {
        return api().create("/enrollments", "{\"course\": \"%s\", \"student\": \"%s\"}".formatted(course, student));
    }

    private void expect(String path, String id, String field, Object value) throws Exception {
        Api.eventually(Duration.ofSeconds(20), () -> api().get(path, id).andExpect(jsonPath(field).value(value)));
    }

    @Test
    void enrollingAndWithdrawingKeepsCountersUpToDate() throws Exception {
        var pottery = course("Pottery");
        var ada = student("ada");
        api().get("/courses/{id}", pottery).andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Pottery"))
                .andExpect(jsonPath("$.enrolled").value(0))
                .andExpect(jsonPath("$.status").value("OPEN"));
        api().get("/students/{id}", ada).andExpect(jsonPath("$.activeEnrollments").value(0));

        var enrollment = enroll(pottery, ada);
        api().get("/enrollments/{id}", enrollment).andExpect(jsonPath("$.status").value("ENROLLED"));
        expect("/courses/{id}", pottery, "$.enrolled", 1);
        expect("/students/{id}", ada, "$.activeEnrollments", 1);

        api().postEmpty("/enrollments/{id}/withdrawal", enrollment).andExpect(status().isOk());
        api().postEmpty("/enrollments/{id}/withdrawal", enrollment).andExpect(status().isOk());
        api().get("/enrollments/{id}", enrollment).andExpect(jsonPath("$.status").value("WITHDRAWN"));
        expect("/courses/{id}", pottery, "$.enrolled", 0);
        expect("/students/{id}", ada, "$.activeEnrollments", 0);
        Thread.sleep(500);
        api().get("/students/{id}", ada).andExpect(jsonPath("$.activeEnrollments").value(0));
    }

    @Test
    void cancellingACourseCancelsItsEnrollments() throws Exception {
        var pottery = course("Pottery");
        var welding = course("Welding");
        var ada = student("ada");
        var grace = student("grace");
        var adaPottery = enroll(pottery, ada);
        var gracePottery = enroll(pottery, grace);
        var adaWelding = enroll(welding, ada);
        api().postEmpty("/enrollments/{id}/withdrawal", gracePottery).andExpect(status().isOk());
        expect("/students/{id}", ada, "$.activeEnrollments", 2);
        expect("/students/{id}", grace, "$.activeEnrollments", 0);

        api().postEmpty("/courses/{id}/cancellation", pottery).andExpect(status().isOk());

        api().get("/courses/{id}", pottery).andExpect(jsonPath("$.status").value("CANCELLED"));
        expect("/enrollments/{id}", adaPottery, "$.status", "CANCELLED");
        expect("/students/{id}", ada, "$.activeEnrollments", 1);
        api().get("/enrollments/{id}", gracePottery).andExpect(jsonPath("$.status").value("WITHDRAWN"));
        api().get("/enrollments/{id}", adaWelding).andExpect(jsonPath("$.status").value("ENROLLED"));
        Thread.sleep(500);
        api().get("/students/{id}", grace).andExpect(jsonPath("$.activeEnrollments").value(0));
    }

    @Test
    void aCourseWithoutEnrollmentsCanBeCancelled() throws Exception {
        var empty = course("Empty");
        var other = course("Other");
        var ada = student("ada");

        api().postEmpty("/courses/{id}/cancellation", empty).andExpect(status().isOk());
        Thread.sleep(500);
        var enrollment = enroll(other, ada);

        api().get("/courses/{id}", empty).andExpect(jsonPath("$.status").value("CANCELLED"));
        expect("/students/{id}", ada, "$.activeEnrollments", 1);
        List<String> listed = Api.read(api().get("/enrollments?course={course}", other).andExpect(status().isOk()),
                "$.content[*].id");
        assertThat(listed).containsExactly(enrollment);
    }
}
