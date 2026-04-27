package be.appify.prefab.example.snssqs.channelsummary;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.EventHandler;
import be.appify.prefab.core.annotations.Multicast;
import be.appify.prefab.core.annotations.rest.Filter;
import be.appify.prefab.core.annotations.rest.GetList;
import be.appify.prefab.core.service.Reference;
import be.appify.prefab.example.snssqs.channel.Channel;
import be.appify.prefab.example.snssqs.channel.ChannelCreated;
import be.appify.prefab.example.snssqs.message.MessageSent;
import be.appify.prefab.example.snssqs.user.UserEvent;
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
    @Multicast(queryMethod = "findByChannel", parameters = "channel")
    public ChannelSummary onUserSubscribed(UserEvent.SubscribedToChannel event) {
        log.info("Handling SubscribedToChannel event for ChannelSummary: {}", event);
        return new ChannelSummary(id, version, channel, name, totalMessages, totalSubscribers + 1);
    }
}
