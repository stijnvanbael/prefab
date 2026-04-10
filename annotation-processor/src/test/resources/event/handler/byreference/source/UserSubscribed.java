package event.handler.byreference;

import be.appify.prefab.core.service.Reference;

public record UserSubscribed(
        Reference<Channel> channel,
        String userId
) {
}
