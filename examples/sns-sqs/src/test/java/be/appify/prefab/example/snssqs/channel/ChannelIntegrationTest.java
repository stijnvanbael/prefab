package be.appify.prefab.example.snssqs.channel;

import be.appify.prefab.core.service.Reference;
import be.appify.prefab.example.snssqs.user.UserClient;
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
        var channelId = channels.createChannel("general").id();
        var johnId = users.createUser("John").id();
        var janeId = users.createUser("Jane").id();
        users.subscribeToChannel(johnId, channelId);
        users.subscribeToChannel(janeId, channelId);

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            var channel = channels.getChannelById(channelId).response();
            assertThat(channel.subscribers()).contains(Reference.fromId(johnId), Reference.fromId(janeId));
        });
    }
}
