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
public record MeteringConfig(
        @Id Reference<MeteringConfig> id,
        String meteringconfig,
        String remark,
        String status
) {
    @Create(path = "/{meteringconfig}/close-for-input")
    public static void closeForInput(String meteringconfig, String remark) {
    }

    @Create(path = "/{meteringconfig}/open-for-input")
    public static void openForInput(String meteringconfig, String remark) {
    }
}

