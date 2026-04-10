package event.handler.multicast;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.EventHandler;
import be.appify.prefab.core.annotations.Multicast;
import be.appify.prefab.core.service.Reference;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Aggregate
public record Channel(
        @Id Reference<Channel> id,
        @Version long version,
        int messageCount
) {
    @EventHandler
    @Multicast(queryMethod = "findByChannel", parameters = "channel")
    public Channel onMessageSent(MessageSent event) {
        return new Channel(id, version, messageCount + 1);
    }
}
