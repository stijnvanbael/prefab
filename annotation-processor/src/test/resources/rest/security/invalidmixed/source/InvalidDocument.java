package rest.security.invalidmixed;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.core.annotations.rest.Security;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Aggregate
public record InvalidDocument(
        @Id String id,
        @Version long version,
        String title
) {
    @Create(security = @Security(authority = "ROLE_EDITOR", role = "EDITOR"))
    public InvalidDocument(String title) {
        this(UUID.randomUUID().toString(), 0L, title);
    }
}
