package be.appify.prefab.example.kafka.user;

import be.appify.prefab.core.service.Reference;
import be.appify.prefab.test.EventConsumer;
import be.appify.prefab.test.IntegrationTest;
import be.appify.prefab.test.TestEventConsumer;
import be.appify.prefab.test.asserts.EventAssertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
class UserIntegrationTest {
    @Autowired
    UserClient userClient;
    @Autowired
    UserExporter userExporter;
    @TestEventConsumer(topic = "${topics.user.name}")
    EventConsumer<UserEvent> userConsumer;

    @Test
    void createUser() throws Exception {
        var userId = userClient.createUser("Alice");

        EventAssertions.assertThat(userConsumer).hasReceivedValueSatisfying(UserEvent.Created.class, userEvent -> {
            assertThat(userEvent.reference()).isNotNull();
            assertThat(userEvent.name()).isEqualTo("Alice");
        });

        assertThat(userExporter.exportedUsers()).extracting(Reference::id).contains(userId);
    }
}

