package be.appify.prefab.example.snssqs.user;

import be.appify.prefab.core.service.Reference;
import be.appify.prefab.test.IntegrationTest;
import be.appify.prefab.test.sns.SqsSubscriber;
import be.appify.prefab.test.sns.TestSqsSubscriber;
import be.appify.prefab.test.sns.asserts.SqsAssertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
class UserIntegrationTest {
    @Autowired
    UserClient userClient;
    @Autowired
    UserExporter userExporter;
    @TestSqsSubscriber(topic = "${topics.user.name}")
    SqsSubscriber<UserEvent> userSubscriber;

    @Test
    void createUser() throws Exception {
        var userId = userClient.createUser("Alice");

        SqsAssertions.assertThat(userSubscriber).hasReceivedValueSatisfying(UserEvent.Created.class, userEvent -> {
            assertThat(userEvent.reference()).isNotNull();
            assertThat(userEvent.name()).isEqualTo("Alice");
        });

        assertThat(userExporter.exportedUsers()).contains(Reference.fromId(userId));
    }
}
