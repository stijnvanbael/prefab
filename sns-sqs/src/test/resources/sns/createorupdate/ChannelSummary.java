package sns.createorupdate;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.ByReference;
import be.appify.prefab.core.annotations.EventHandler;
import be.appify.prefab.core.service.Reference;
import org.springframework.data.annotation.Id;

@Aggregate
public record ChannelSummary(
        @Id Reference<ChannelSummary> id,
        int messageCount
) {
    @EventHandler
    public static ChannelSummary onCreate(MessageEvent.Sent event) {
        return new ChannelSummary(event.summary(), 1);
    }

    @EventHandler
    @ByReference(property = "summary")
    public ChannelSummary onUpdate(MessageEvent.Sent event) {
        return new ChannelSummary(id, messageCount + 1);
    }
}

