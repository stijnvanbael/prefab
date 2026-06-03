package dbmigration.longidentifiers;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.DbMigration;
import be.appify.prefab.core.annotations.Generate;
import be.appify.prefab.core.annotations.rest.Filter;
import be.appify.prefab.core.service.Reference;
import be.appify.prefab.processor.assertion.AssertionPlugin;
import be.appify.prefab.processor.mother.MotherPlugin;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Aggregate
@DbMigration
@Generate(plugin = MotherPlugin.class, enabled = false)
@Generate(plugin = AssertionPlugin.class, enabled = false)
public record OrderProjection(
        @Id Reference<OrderProjection> id,
        @Version long version,
        @NotNull CustomerContextInformation customerContextInformation
) {
    public record CustomerContextInformation(
            @Filter @NotNull String legalRepresentativeDisplayNameForDocumentsAndNotifications,
            @NotNull String preferredLocale
    ) {
    }
}


