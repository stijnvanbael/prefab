package event.handler.multicastcreateorupdate;

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
    public static Channel onCreate(MessageSent event) {
        return new Channel(Reference.create(), 0L, 1);
    }

    @EventHandler
    @Multicast(queryMethod = "findByChannel", parameters = "channel")
    public Channel onMessageSent(MessageSent event) {
        return new Channel(id, version, messageCount + 1);
    }
}
