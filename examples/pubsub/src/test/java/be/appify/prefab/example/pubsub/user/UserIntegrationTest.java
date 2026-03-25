package be.appify.prefab.example.pubsub.user;

import be.appify.prefab.core.service.Reference;
import be.appify.prefab.test.IntegrationTest;
import be.appify.prefab.test.pubsub.Subscriber;
import be.appify.prefab.test.pubsub.TestSubscriber;
import be.appify.prefab.test.pubsub.asserts.PubSubAssertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
class UserIntegrationTest {
    @Autowired
    UserClient userClient;
    @Autowired
    UserExporter userExporter;
    @TestSubscriber(topic = "${topics.user.name}")
    Subscriber<UserEvent> userSubscriber;

    @Test
    void createUser() throws Exception {
        var userId = userClient.createUser("Alice");

        PubSubAssertions.assertThat(userSubscriber).hasReceivedValueSatisfying(UserEvent.Created.class, userEvent -> {
            assertThat(userEvent.reference()).isNotNull();
            assertThat(userEvent.name()).isEqualTo("Alice");
        });

        assertThat(userExporter.exportedUsers()).contains(Reference.fromId(userId));
    }
}
