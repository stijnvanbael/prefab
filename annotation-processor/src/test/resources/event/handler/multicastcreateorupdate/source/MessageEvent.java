package event.handler.multicastcreateorupdate;

import be.appify.prefab.core.service.Reference;

public interface MessageEvent {
    Reference<Channel> channel();
}

