package event.handler.createorupdate;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.CreateOrUpdate;
import be.appify.prefab.core.service.Reference;
import java.util.Optional;
import org.springframework.data.annotation.Id;

@Aggregate
public record ChannelSummary(
        @Id Reference<ChannelSummary> id,
        int messageCount
) {
    @CreateOrUpdate(property = "summary")
    public static ChannelSummary onMessageSent(Optional<ChannelSummary> existing, MessageSent event) {
        return existing
                .map(cs -> new ChannelSummary(cs.id(), cs.messageCount() + 1))
                .orElseGet(() -> new ChannelSummary(event.summary(), 1));
    }
}
