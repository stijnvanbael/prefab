package be.appify.prefab.example.snssqs.channelsummary;

import be.appify.prefab.example.snssqs.channel.ChannelClient;
import be.appify.prefab.example.snssqs.message.MessageClient;
import be.appify.prefab.example.snssqs.user.UserClient;
import be.appify.prefab.test.IntegrationTest;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@IntegrationTest
class ChannelSummaryIntegrationTest {

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
        for (int run = 1; run <= 20; run++) {
            assertScenario("general-" + run, run);
        }
    }

    private void assertScenario(String channelName, int run) throws Exception {
        var channelId = channels.createChannel(channelName);
        var johnId = users.createUser("John-" + run);
        var janeId = users.createUser("Jane-" + run);
        var daveId = users.createUser("Dave-" + run);
        users.subscribeToChannel(johnId, channelId);
        users.subscribeToChannel(janeId, channelId);
        users.subscribeToChannel(daveId, channelId);

        messages.createMessage(johnId, channelId, "Hello, World!");
        messages.createMessage(janeId, channelId, "Hello, John!");

        await().atMost(20, TimeUnit.SECONDS).untilAsserted(() ->
                assertThat(channelSummaries.findChannelSummaries(Pageable.unpaged(), channelName))
                        .anySatisfy(summary -> {
                            assertThat(summary.totalSubscribers()).isEqualTo(3);
                            assertThat(summary.totalMessages()).isEqualTo(2);
                        }));
    }
}
