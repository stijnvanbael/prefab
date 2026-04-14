package event.handler.createorupdate;

import be.appify.prefab.core.service.Reference;

public record MessageSent(
        Reference<ChannelSummary> summary,
        String userId,
        String text
) {
}
