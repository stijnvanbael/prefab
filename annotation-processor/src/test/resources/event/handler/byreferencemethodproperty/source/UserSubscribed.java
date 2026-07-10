package event.handler.byreferencemethodproperty;

import be.appify.prefab.core.annotations.Generate;
import be.appify.prefab.core.service.Reference;
import be.appify.prefab.processor.assertion.AssertionPlugin;
import be.appify.prefab.processor.mother.MotherPlugin;

@Generate(plugin = MotherPlugin.class, enabled = false)
@Generate(plugin = AssertionPlugin.class, enabled = false)
public class UserSubscribed {
    private final Reference<Channel> channelRef;
    private final String userId;

    public UserSubscribed(Reference<Channel> channelRef, String userId) {
        this.channelRef = channelRef;
        this.userId = userId;
    }

    public Reference<Channel> channelId() {
        return channelRef;
    }

    public String userId() {
        return userId;
    }
}

