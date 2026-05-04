package be.appify.prefab.example.kafka.channelsummary;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.EventHandler;
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
        @Id Reference<ChannelSummary>  id,
        @Version long version,
        Reference<Channel> channel,
        @Filter @Size(max = 255) String name,
        int totalMessages,
        int totalSubscribers
) {
    private static final Logger log = LoggerFactory.getLogger(ChannelSummary.class);
    private static final String PENDING_NAME = "";

    /**
     * Updates the channel name on an existing summary document.
     * Handles the case where {@link MessageSent} or subscription events arrive before
     * {@link ChannelCreated}. When no document exists yet, the static companion
     * {@link #onChannelCreated} creates a fresh one.
     */
    @EventHandler
    @Multicast(queryMethod = "findByChannel", parameters = "reference")
    public ChannelSummary updateChannelName(ChannelCreated event) {
        return new ChannelSummary(id, version, channel, event.name(), totalMessages, totalSubscribers);
    }

    /**
     * Creates a new summary document when a channel is first created and no summary exists yet.
     * Acts as the static companion for {@link #updateChannelName}.
     */
    @EventHandler
    public static ChannelSummary onChannelCreated(ChannelCreated event) {
        return new ChannelSummary(Reference.create(), 0L, event.reference(), event.name(), 0, 0);
    }

    /**
     * Increments the message counter whenever a message is sent to this channel.
     * When no summary exists yet, the static companion {@link #createFromMessage} creates a
     * partial document that will be updated with the channel name once {@link ChannelCreated}
     * arrives.
     */
    @EventHandler
    @Multicast(queryMethod = "findByChannel", parameters = "channel")
    public ChannelSummary onMessageSent(MessageSent event) {
        log.info("Handling MessageSent event for ChannelSummary: {}", event);
        return new ChannelSummary(id, version, channel, name, totalMessages + 1, totalSubscribers);
    }

    /**
     * Creates a partial summary document when {@link MessageSent} arrives before
     * {@link ChannelCreated}. The {@link #name} field is left as a placeholder and will be filled
     * in by {@link #updateChannelName} once the channel event is processed.
     */
    @EventHandler
    public static ChannelSummary createFromMessage(MessageSent event) {
        return new ChannelSummary(Reference.create(), 0L, event.channel(), PENDING_NAME, 1, 0);
    }

    /**
     * Increments the subscriber counter whenever a user subscribes to this channel.
     * When no summary exists yet, the static companion {@link #createFromSubscription} creates a
     * partial document.
     */
    @EventHandler
    @Multicast(queryMethod = "findByChannel", parameters = "channel")
    public ChannelSummary onUserSubscribed(UserEvent.SubscribedToChannel event) {
        log.info("Handling SubscribedToChannel event for ChannelSummary: {}", event);
        return new ChannelSummary(id, version, channel, name, totalMessages, totalSubscribers + 1);
    }

    /**
     * Creates a partial summary document when a subscription event arrives before
     * {@link ChannelCreated}. The {@link #name} field is left as a placeholder and will be filled
     * in by {@link #updateChannelName} once the channel event is processed.
     */
    @EventHandler
    public static ChannelSummary createFromSubscription(UserEvent.SubscribedToChannel event) {
        return new ChannelSummary(Reference.create(), 0L, event.channel(), PENDING_NAME, 0, 1);
    }
}
