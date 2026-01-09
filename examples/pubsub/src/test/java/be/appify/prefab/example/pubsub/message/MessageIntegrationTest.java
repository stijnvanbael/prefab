package be.appify.prefab.example.pubsub.message;

import org.junit.jupiter.api.Test;

import be.appify.prefab.core.service.Reference;
import be.appify.prefab.example.pubsub.channel.ChannelClient;
import be.appify.prefab.example.pubsub.channel.application.CreateChannelRequest;
import be.appify.prefab.example.pubsub.message.application.CreateMessageRequest;
import be.appify.prefab.example.pubsub.user.UnreadMessage;
import be.appify.prefab.example.pubsub.user.UserClient;
import be.appify.prefab.example.pubsub.user.UserStatusClient;
import be.appify.prefab.example.pubsub.user.application.CreateUserRequest;
import be.appify.prefab.example.pubsub.user.application.UserSubscribeToChannelRequest;
import be.appify.prefab.example.pubsub.user.infrastructure.http.UserStatusResponse;
import be.appify.prefab.test.IntegrationTest;
import be.appify.prefab.test.pubsub.PubSubContainerSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@IntegrationTest
class MessageIntegrationTest implements PubSubContainerSupport {

    @Autowired
    ChannelClient channels;
    @Autowired
    UserClient users;
    @Autowired
    UserStatusClient userStatuses;
    @Autowired
    MessageClient messages;

    @Test
    void addMessageToUnreadMessagesOnAllUsersInChannel() throws Exception {
        var channelId = channels.createChannel(new CreateChannelRequest("general"));
        var johnId = users.createUser(new CreateUserRequest("John"));
        var janeId = users.createUser(new CreateUserRequest("Jane"));
        var daveId = users.createUser(new CreateUserRequest("Dave"));
        users.subscribeToChannel(johnId, new UserSubscribeToChannelRequest(channelId));
        users.subscribeToChannel(janeId, new UserSubscribeToChannelRequest(channelId));

        var messageId = messages.createMessage(new CreateMessageRequest(johnId, channelId, "Hello, World!"));

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() ->
                assertThat(userStatuses.findUserStatuses(Pageable.unpaged()).getContent())
                        .extracting(UserStatusResponse::unreadMessages)
                        .contains(
                                List.of(new UnreadMessage(Reference.fromId(messageId), Reference.fromId(channelId))),
                                List.of(new UnreadMessage(Reference.fromId(messageId), Reference.fromId(channelId)))
                        ));

        var dave = userStatuses.findUserStatuses(Pageable.unpaged()).getContent()
                .stream()
                .filter(userStatus -> daveId.equals(userStatus.user().id())).findFirst();
        assertThat(dave).hasValueSatisfying(u ->
                assertThat(u.unreadMessages()).isEmpty());
    }
}
