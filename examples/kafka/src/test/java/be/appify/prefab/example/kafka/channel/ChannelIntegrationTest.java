package be.appify.prefab.example.kafka.channel;

import org.junit.jupiter.api.Test;

import be.appify.prefab.example.kafka.channel.application.CreateChannelRequest;
import be.appify.prefab.example.kafka.user.UserFixture;
import be.appify.prefab.example.kafka.user.application.CreateUserRequest;
import be.appify.prefab.example.kafka.user.application.UserSubscribeToChannelRequest;
import be.appify.prefab.test.IntegrationTest;
import be.appify.prefab.test.kafka.KafkaContainerSupport;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@IntegrationTest
class ChannelIntegrationTest implements KafkaContainerSupport {
    @Autowired
    ChannelFixture channels;
    @Autowired
    UserFixture users;

    @Test
    void subscribeToChannel() throws Exception {
        var channelId = channels.createChannel(new CreateChannelRequest("general"));
        var johnId = users.createUser(new CreateUserRequest("John"));
        var janeId = users.createUser(new CreateUserRequest("Jane"));
        users.subscribeToChannel(johnId, new UserSubscribeToChannelRequest(channelId));
        users.subscribeToChannel(janeId, new UserSubscribeToChannelRequest(channelId));

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            var channel = channels.getChannelById(channelId);
            assertThat(channel.subscribers()).contains(johnId, janeId);
        });
    }
}
