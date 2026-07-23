package event.handler.multicastcreateorupdate;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.EventHandler;
import be.appify.prefab.core.annotations.Generate;
import be.appify.prefab.core.annotations.Multicast;
import be.appify.prefab.core.service.Reference;
import be.appify.prefab.processor.assertion.AssertionPlugin;
import be.appify.prefab.processor.dbmigration.DbMigrationPlugin;
import be.appify.prefab.processor.mother.MotherPlugin;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Aggregate
@Generate(plugin = MotherPlugin.class, enabled = false)
@Generate(plugin = AssertionPlugin.class, enabled = false)
@Generate(plugin = DbMigrationPlugin.class, enabled = false)
public record Channel(
        @Id Reference<Channel> id,
        @Version long version,
        int messageCount
) {
    @EventHandler
    public static Channel onCreate(MessageEvent event) {
        return new Channel(Reference.create(), 0L, 1);
    }

    @EventHandler
    @Multicast(queryMethod = "findByChannel", parameters = "channel")
    public Channel onMessageSent(MessageEvent event) {
        return new Channel(id, version, messageCount + 1);
    }
}
