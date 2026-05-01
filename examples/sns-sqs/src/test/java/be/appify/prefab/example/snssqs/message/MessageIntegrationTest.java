package be.appify.prefab.example.snssqs.message;

import be.appify.prefab.core.service.Reference;
import be.appify.prefab.example.snssqs.channel.ChannelClient;
import be.appify.prefab.example.snssqs.user.UnreadMessage;
import be.appify.prefab.example.snssqs.user.UserClient;
import be.appify.prefab.example.snssqs.user.UserStatusClient;
import be.appify.prefab.example.snssqs.user.infrastructure.http.UserStatusResponse;
import be.appify.prefab.test.IntegrationTest;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@IntegrationTest
class MessageIntegrationTest {

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
        var channelId = channels.createChannel("general");
        var johnId = users.createUser("John");
        var janeId = users.createUser("Jane");
        var daveId = users.createUser("Dave");
        users.subscribeToChannel(johnId, channelId);
        users.subscribeToChannel(janeId, channelId);

        var messageId = messages.createMessage(johnId, channelId, "Hello, World!");

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
