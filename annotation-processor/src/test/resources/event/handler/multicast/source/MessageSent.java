package event.handler.multicast;

import be.appify.prefab.core.service.Reference;

public record MessageSent(
        Reference<Channel> channel,
        String content
) {
}
