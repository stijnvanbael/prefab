package rest.asyncmultiplecreatewithpathvar;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.AsyncCommit;
import be.appify.prefab.core.annotations.Generate;
import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.core.service.Reference;
import be.appify.prefab.processor.assertion.AssertionPlugin;
import be.appify.prefab.processor.dbmigration.DbMigrationPlugin;
import be.appify.prefab.processor.mother.MotherPlugin;
import org.springframework.data.annotation.Id;

@Aggregate
@AsyncCommit
@Generate(plugin = MotherPlugin.class, enabled = false)
@Generate(plugin = AssertionPlugin.class, enabled = false)
@Generate(plugin = DbMigrationPlugin.class, enabled = false)
public record Configuration(
        @Id Reference<Configuration> id,
        String configurationId,
        String remark,
        String status
) {
    @Create(path = "/configurations/{configurationId}/disable")
    public static void disable(String configurationId, String remark) {
    }

    @Create(path = "/configurations/{configurationId}/enable")
    public static void enable(String configurationId, String remark) {
    }
}
