package be.appify.prefab.example.kafka.user;

import org.junit.jupiter.api.Test;

import be.appify.prefab.example.kafka.user.application.CreateUserRequest;
import be.appify.prefab.test.IntegrationTest;
import be.appify.prefab.test.kafka.TestConsumer;
import be.appify.prefab.test.kafka.asserts.KafkaAssertions;
import org.apache.kafka.clients.consumer.Consumer;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
class UserIntegrationTest {
    @Autowired
    UserClient userClient;
    @Autowired
    UserExporter userExporter;
    @TestConsumer(topic = "${topics.user.name}")
    Consumer<String, UserEvent> userConsumer;

    @Test
    void createUser() throws Exception {
        var userId = userClient.createUser(new CreateUserRequest("Alice"));

        KafkaAssertions.assertThat(userConsumer).hasReceivedValueSatisfying(UserEvent.Created.class, userEvent -> {
            assertThat(userEvent.id()).isNotNull();
            assertThat(userEvent.name()).isEqualTo("Alice");
        });

        assertThat(userExporter.exportedUserIds()).contains(userId);
    }
}
