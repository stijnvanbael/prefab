package rest.childscope;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.Generate;
import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.core.annotations.rest.Delete;
import be.appify.prefab.core.annotations.rest.GetById;
import be.appify.prefab.core.annotations.rest.Parent;
import be.appify.prefab.core.annotations.rest.Update;
import be.appify.prefab.core.service.Reference;
import be.appify.prefab.processor.assertion.AssertionPlugin;
import be.appify.prefab.processor.dbmigration.DbMigrationPlugin;
import be.appify.prefab.processor.mother.MotherPlugin;
import org.springframework.data.annotation.Id;

@Aggregate
@GetById
@Delete
@Generate(plugin = MotherPlugin.class, enabled = false)
@Generate(plugin = AssertionPlugin.class, enabled = false)
@Generate(plugin = DbMigrationPlugin.class, enabled = false)
public record Step(
        @Id Reference<Step> id,
        @Parent Reference<Plan> planId,
        String title) {

    @Create
    public Step(Reference<Plan> planId, String title) {
        this(Reference.create(), planId, title);
    }

    @Update
    public Step rename(String title) {
        return new Step(id, planId, title);
    }
}
