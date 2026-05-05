package be.appify.prefab.example.pubsub.channelsummary;

import org.junit.jupiter.api.Test;

import be.appify.prefab.example.pubsub.channel.ChannelClient;
import be.appify.prefab.example.pubsub.message.MessageClient;
import be.appify.prefab.example.pubsub.user.UserClient;
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
        var channelId = channels.createChannel("general");
        var johnId = users.createUser("John");
        var janeId = users.createUser("Jane");
        var daveId = users.createUser("Dave");
        users.subscribeToChannel(johnId, channelId);
        users.subscribeToChannel(janeId, channelId);
        users.subscribeToChannel(daveId, channelId);

        messages.createMessage(johnId, channelId, "Hello, World!");
        messages.createMessage(janeId, channelId, "Hello, John!");

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() ->
                assertThat(channelSummaries.findChannelSummaries(Pageable.unpaged(), "general"))
                        .anySatisfy(summary -> {
                            assertThat(summary.totalSubscribers()).isEqualTo(3);
                            assertThat(summary.totalMessages()).isEqualTo(2);
                        }));
    }
}
