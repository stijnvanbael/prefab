package be.appify.prefab.example.pubsub.channelsummary;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.DbMigration;
import be.appify.prefab.core.annotations.EventHandler;
import be.appify.prefab.core.annotations.rest.Filter;
import be.appify.prefab.core.annotations.rest.GetList;
import be.appify.prefab.core.service.Reference;
import be.appify.prefab.example.pubsub.channel.Channel;
import be.appify.prefab.example.pubsub.channel.ChannelCreated;
import be.appify.prefab.example.pubsub.message.MessageSent;
import be.appify.prefab.example.pubsub.user.UserEvent;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.annotation.Version;

import java.util.UUID;

@Aggregate
@GetList
@DbMigration
public record ChannelSummary(
        @Id String id,
        @Version long version,
        @NotNull Reference<Channel> channel,
        @Filter @NotNull String name,
        int totalMessages,
        int totalSubscribers
) {
    private static final Logger log = LoggerFactory.getLogger(ChannelSummary.class);

    @PersistenceCreator
    public ChannelSummary {
    }

    @EventHandler
    public static ChannelSummary onChannelCreated(ChannelCreated event) {
        return new ChannelSummary(UUID.randomUUID().toString(), 0L, Reference.fromId(event.id()), event.name(), 0, 0);
    }

    @EventHandler.Multicast(queryMethod = "findByChannel", paramMapping = @EventHandler.Param(from = "channel", to = "channel"))
    public ChannelSummary onMessageSent(MessageSent event) {
        log.info("Handling MessageSent event for ChannelSummary: {}", event);
        return new ChannelSummary(id, version, channel, name, totalMessages + 1, totalSubscribers);
    }

    @EventHandler.Multicast(queryMethod = "findByChannel", paramMapping = @EventHandler.Param(from = "channel", to = "channel"))
    public ChannelSummary onUserSubscribed(UserEvent.SubscribedToChannel event) {
        log.info("Handling SubscribedToChannel event for ChannelSummary: {}", event);
        return new ChannelSummary(id, version, channel, name, totalMessages, totalSubscribers + 1);
    }
}
