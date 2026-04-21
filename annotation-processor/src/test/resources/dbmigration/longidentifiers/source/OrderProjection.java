package dbmigration.longidentifiers;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.DbMigration;
import be.appify.prefab.core.annotations.rest.Filter;
import be.appify.prefab.core.service.Reference;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Aggregate
@DbMigration
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


