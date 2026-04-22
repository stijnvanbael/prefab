package kafka.createorupdate;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.service.Reference;

@Event(topic = "${topic.message.name}", platform = Event.Platform.KAFKA)
public sealed interface MessageEvent permits MessageEvent.Sent {
    record Sent(
            Reference<ChannelSummary> summary,
            String text
    ) implements MessageEvent {
    }
}

