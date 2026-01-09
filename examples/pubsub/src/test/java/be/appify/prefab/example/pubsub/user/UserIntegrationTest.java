package be.appify.prefab.example.pubsub.user;

import org.junit.jupiter.api.Test;

import be.appify.prefab.example.pubsub.user.application.CreateUserRequest;
import be.appify.prefab.test.IntegrationTest;
import be.appify.prefab.test.pubsub.PubSubContainerSupport;
import be.appify.prefab.test.pubsub.Subscriber;
import be.appify.prefab.test.pubsub.TestSubscriber;
import be.appify.prefab.test.pubsub.asserts.PubSubAssertions;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
class UserIntegrationTest implements PubSubContainerSupport {
    @Autowired
    UserClient userClient;
    @Autowired
    UserExporter userExporter;
    @TestSubscriber(topic = "${topics.user.name}")
    Subscriber<UserEvent> userSubscriber;

    @Test
    void createUser() throws Exception {
        var userId = userClient.createUser(new CreateUserRequest("Alice"));

        PubSubAssertions.assertThat(userSubscriber).hasReceivedValueSatisfying(UserEvent.Created.class, userEvent -> {
            assertThat(userEvent.id()).isNotNull();
            assertThat(userEvent.name()).isEqualTo("Alice");
        });

        assertThat(userExporter.exportedUserIds()).contains(userId);
    }
}
