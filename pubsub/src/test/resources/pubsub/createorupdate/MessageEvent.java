package pubsub.createorupdate;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.service.Reference;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
@Event(topic = "${topic.message.name}", platform = Event.Platform.PUB_SUB)
public sealed interface MessageEvent permits MessageEvent.Sent {
    record Sent(
            Reference<ChannelSummary> summary,
            String text
    ) implements MessageEvent {
    }
}

