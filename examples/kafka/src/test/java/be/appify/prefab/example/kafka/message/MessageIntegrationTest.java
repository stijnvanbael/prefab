package be.appify.prefab.example.kafka.message;

import org.junit.jupiter.api.Test;

import be.appify.prefab.core.service.Reference;
import be.appify.prefab.example.kafka.channel.ChannelFixture;
import be.appify.prefab.example.kafka.channel.application.CreateChannelRequest;
import be.appify.prefab.example.kafka.message.application.CreateMessageRequest;
import be.appify.prefab.example.kafka.user.UnreadMessage;
import be.appify.prefab.example.kafka.user.UserFixture;
import be.appify.prefab.example.kafka.user.application.CreateUserRequest;
import be.appify.prefab.example.kafka.user.application.UserSubscribeToChannelRequest;
import be.appify.prefab.test.IntegrationTest;
import be.appify.prefab.test.kafka.KafkaContainerSupport;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.concurrent.TimeUnit;

import static be.appify.prefab.test.kafka.asserts.KafkaAssertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.awaitility.Awaitility.await;

@IntegrationTest
class MessageIntegrationTest implements KafkaContainerSupport {

    @Autowired
    ChannelFixture channels;
    @Autowired
    UserFixture users;
    @Autowired
    MessageFixture messages;

    @Test
    void addMessageToUnreadMessagesOnAllUsersInChannel() throws Exception {
        var channelId = channels.createChannel(new CreateChannelRequest("general"));
        var johnId = users.createUser(new CreateUserRequest("John"));
        var janeId = users.createUser(new CreateUserRequest("Jane"));
        var daveId = users.createUser(new CreateUserRequest("Dave"));
        users.subscribeToChannel(johnId, new UserSubscribeToChannelRequest(channelId));
        users.subscribeToChannel(janeId, new UserSubscribeToChannelRequest(channelId));

        var messageId = messages.createMessage(new CreateMessageRequest(johnId, channelId, "Hello, World!"));

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            var john = users.getUserById(johnId);
            var jane = users.getUserById(janeId);

            assertThat(john.unreadMessages()).extracting(UnreadMessage::message, UnreadMessage::channel)
                    .containsExactly(tuple(Reference.fromId(messageId), Reference.fromId(channelId)));
            assertThat(jane.unreadMessages()).extracting(UnreadMessage::message, UnreadMessage::channel)
                    .containsExactly(tuple(Reference.fromId(messageId), Reference.fromId(channelId)));
        });

        var dave = users.getUserById(daveId);
        assertThat(dave.unreadMessages()).isEmpty();
    }
}
