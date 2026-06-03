package event.handler.createorupdate;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.ByReference;
import be.appify.prefab.core.annotations.EventHandler;
import be.appify.prefab.core.annotations.Generate;
import be.appify.prefab.core.service.Reference;
import be.appify.prefab.processor.assertion.AssertionPlugin;
import be.appify.prefab.processor.dbmigration.DbMigrationPlugin;
import be.appify.prefab.processor.mother.MotherPlugin;
import org.springframework.data.annotation.Id;

@Aggregate
@Generate(plugin = MotherPlugin.class, enabled = false)
@Generate(plugin = AssertionPlugin.class, enabled = false)
@Generate(plugin = DbMigrationPlugin.class, enabled = false)
public record ChannelSummary(
        @Id Reference<ChannelSummary> id,
        int messageCount
) {
    @EventHandler
    public static ChannelSummary onCreate(MessageSent event) {
        return new ChannelSummary(event.summary(), 1);
    }

    @EventHandler
    @ByReference(property = "summary")
    public ChannelSummary onUpdate(MessageSent event) {
        return new ChannelSummary(id, messageCount + 1);
    }
}
