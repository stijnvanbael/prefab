package be.appify.prefab.example.kafka.channelsummary;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.EventHandler;
import be.appify.prefab.core.annotations.Indexed;
import be.appify.prefab.core.annotations.Multicast;
import be.appify.prefab.core.annotations.rest.Filter;
import be.appify.prefab.core.annotations.rest.GetList;
import be.appify.prefab.core.service.Reference;
import be.appify.prefab.example.kafka.channel.Channel;
import be.appify.prefab.example.kafka.channel.ChannelCreated;
import be.appify.prefab.example.kafka.message.MessageSent;
import be.appify.prefab.example.kafka.user.UserEvent;
import jakarta.validation.constraints.Size;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Aggregate
@GetList
public record ChannelSummary(
        @Id Reference<ChannelSummary> id,
        @Version long version,
        @Indexed(unique = true) Reference<Channel> channel,
        @Filter @Size(max = 255) String name,
        int totalMessages,
        int totalSubscribers
) {
    private static final Logger log = LoggerFactory.getLogger(ChannelSummary.class);
    private static final String PENDING_NAME = "";

    @EventHandler
    @Multicast(queryMethod = "findByChannel", parameters = "reference")
    public ChannelSummary updateChannelName(ChannelCreated event) {
        return new ChannelSummary(id, version, channel, event.name(), totalMessages, totalSubscribers);
    }

    @EventHandler
    public static ChannelSummary onChannelCreated(ChannelCreated event) {
        return new ChannelSummary(Reference.create(), 0L, event.reference(), event.name(), 0, 0);
    }

    @EventHandler
    @Multicast(queryMethod = "findByChannel", parameters = "channel")
    public ChannelSummary onMessageSent(MessageSent event) {
        log.info("Handling MessageSent event for ChannelSummary: {}", event);
        return new ChannelSummary(id, version, channel, name, totalMessages + 1, totalSubscribers);
    }

    @EventHandler
    public static ChannelSummary createFromMessage(MessageSent event) {
        return new ChannelSummary(Reference.create(), 0L, event.channel(), PENDING_NAME, 1, 0);
    }

    @EventHandler
    @Multicast(queryMethod = "findByChannel", parameters = "channel")
    public ChannelSummary onUserSubscribed(UserEvent.SubscribedToChannel event) {
        log.info("Handling SubscribedToChannel event for ChannelSummary: {}", event);
        return new ChannelSummary(id, version, channel, name, totalMessages, totalSubscribers + 1);
    }

    @EventHandler
    public static ChannelSummary createFromSubscription(UserEvent.SubscribedToChannel event) {
        return new ChannelSummary(Reference.create(), 0L, event.channel(), PENDING_NAME, 0, 1);
    }
}
