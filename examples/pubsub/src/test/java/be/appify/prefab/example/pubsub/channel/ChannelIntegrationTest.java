package be.appify.prefab.example.pubsub.channel;

import be.appify.prefab.core.service.Reference;
import be.appify.prefab.example.pubsub.channel.application.CreateChannelRequest;
import be.appify.prefab.example.pubsub.user.UserClient;
import be.appify.prefab.example.pubsub.user.application.CreateUserRequest;
import be.appify.prefab.example.pubsub.user.application.UserSubscribeToChannelRequest;
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
        var channelId = channels.createChannel(new CreateChannelRequest("general"));
        var johnId = users.createUser(new CreateUserRequest("John"));
        var janeId = users.createUser(new CreateUserRequest("Jane"));
        users.subscribeToChannel(johnId, new UserSubscribeToChannelRequest(channelId));
        users.subscribeToChannel(janeId, new UserSubscribeToChannelRequest(channelId));

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            var channel = channels.getChannelById(channelId);
            assertThat(channel.subscribers()).contains(Reference.fromId(johnId), Reference.fromId(janeId));
        });
    }
}
