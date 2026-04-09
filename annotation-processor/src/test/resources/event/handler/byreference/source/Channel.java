package event.handler.byreference;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.ByReference;
import be.appify.prefab.core.annotations.EventHandler;
import be.appify.prefab.core.service.Reference;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Aggregate
public record Channel(
        @Id Reference<Channel> id,
        @Version long version,
        List<String> subscribers
) {
    @EventHandler
    @ByReference(property = "channel")
    public void onUserSubscribed(UserSubscribed event) {
        subscribers.add(event.userId());
    }
}
