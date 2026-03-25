package be.appify.prefab.example.kafka.channel;

import be.appify.prefab.core.service.Reference;
import be.appify.prefab.example.kafka.user.UserClient;
import be.appify.prefab.test.IntegrationTest;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@IntegrationTest
class ChannelIntegrationTest {
    @Autowired
    ChannelClient channels;
    @Autowired
    UserClient users;

    @Test
    void subscribeToChannel() throws Exception {
        var channelId = channels.createChannel("general");
        var johnId = users.createUser("John");
        var janeId = users.createUser("Jane");
        users.subscribeToChannel(johnId, channelId);
        users.subscribeToChannel(janeId, channelId);

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            var channel = channels.getChannelById(channelId);
            assertThat(channel.subscribers()).extracting(Reference::id).contains(johnId, janeId);
        });
    }
}
