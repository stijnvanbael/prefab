package be.appify.prefab.example.pubsub.channelsummary;

import org.junit.jupiter.api.Test;

import be.appify.prefab.example.pubsub.channel.ChannelClient;
import be.appify.prefab.example.pubsub.channel.application.CreateChannelRequest;
import be.appify.prefab.example.pubsub.message.MessageClient;
import be.appify.prefab.example.pubsub.message.application.CreateMessageRequest;
import be.appify.prefab.example.pubsub.user.UserClient;
import be.appify.prefab.example.pubsub.user.application.CreateUserRequest;
import be.appify.prefab.example.pubsub.user.application.UserSubscribeToChannelRequest;
import be.appify.prefab.test.IntegrationTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@IntegrationTest
public class ChannelSummaryIntegrationTest {

    @Autowired
    ChannelClient channels;
    @Autowired
    UserClient users;
    @Autowired
    MessageClient messages;
    @Autowired
    ChannelSummaryClient channelSummaries;

    @Test
    void updateChannelSummaryTotals() throws Exception {
        var channelId = channels.createChannel(new CreateChannelRequest("general"));
        var johnId = users.createUser(new CreateUserRequest("John"));
        var janeId = users.createUser(new CreateUserRequest("Jane"));
        var daveId = users.createUser(new CreateUserRequest("Dave"));
        users.subscribeToChannel(johnId, new UserSubscribeToChannelRequest(channelId));
        users.subscribeToChannel(janeId, new UserSubscribeToChannelRequest(channelId));
        users.subscribeToChannel(daveId, new UserSubscribeToChannelRequest(channelId));

        messages.createMessage(new CreateMessageRequest(johnId, channelId, "Hello, World!"));
        messages.createMessage(new CreateMessageRequest(janeId, channelId, "Hello, John!"));

        await().atMost(15, TimeUnit.SECONDS).untilAsserted(() ->
                assertThat(channelSummaries.findChannelSummaries(Pageable.unpaged(), "general"))
                        .anySatisfy(summary -> {
                            assertThat(summary.totalSubscribers()).isEqualTo(3);
                            assertThat(summary.totalMessages()).isEqualTo(2);
                        }));
    }
}
