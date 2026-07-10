package event.handler.byreferencemethodproperty;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.ByReference;
import be.appify.prefab.core.annotations.EventHandler;
import be.appify.prefab.core.annotations.Generate;
import be.appify.prefab.core.service.Reference;
import be.appify.prefab.processor.assertion.AssertionPlugin;
import be.appify.prefab.processor.dbmigration.DbMigrationPlugin;
import be.appify.prefab.processor.mother.MotherPlugin;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Aggregate
@Generate(plugin = MotherPlugin.class, enabled = false)
@Generate(plugin = AssertionPlugin.class, enabled = false)
@Generate(plugin = DbMigrationPlugin.class, enabled = false)
public record Channel(
        @Id Reference<Channel> id,
        @Version long version,
        List<String> subscribers
) {
    @EventHandler
    @ByReference(property = "channelId")
    public void onUserSubscribed(UserSubscribed event) {
        subscribers.add(event.userId());
    }
}

