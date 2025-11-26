package be.appify.prefab.example.sale;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.DbMigration;
import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.core.annotations.rest.GetById;
import be.appify.prefab.core.annotations.rest.Security;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Embedded;

import java.util.UUID;

@Aggregate
@GetById(security = @Security(authority = "customer:view"))
@DbMigration
public record Customer(
        @Id String id,
        @Version long version,
        @NotNull @Embedded.Nullable(prefix = "name_") PersonName name,
        @NotNull @Embedded.Nullable(prefix = "address_") Address address,
        @NotNull String email
) {
    @Create(security = @Security(authority = "customer:create"))
    public Customer(
            @NotNull PersonName name,
            @NotNull Address address,
            @NotNull String email
    ) {
        this(UUID.randomUUID().toString(), 0, name, address, email);
    }
}
