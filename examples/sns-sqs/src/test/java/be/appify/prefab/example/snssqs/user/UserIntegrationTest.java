package be.appify.prefab.example.snssqs.user;

import be.appify.prefab.core.service.Reference;
import be.appify.prefab.example.snssqs.user.application.CreateUserRequest;
import be.appify.prefab.test.IntegrationTest;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@IntegrationTest
class UserIntegrationTest {
    @Autowired
    UserClient userClient;
    @Autowired
    UserExporter userExporter;

    @Test
    void createUser() throws Exception {
        var userId = userClient.createUser(new CreateUserRequest("Alice"));

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() ->
                assertThat(userExporter.exportedUsers()).contains(Reference.fromId(userId)));
    }
}
