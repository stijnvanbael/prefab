package event.handler.createorupdate;

import be.appify.prefab.core.service.Reference;

public interface MessageEvent {
    Reference<ChannelSummary> summary();
}

