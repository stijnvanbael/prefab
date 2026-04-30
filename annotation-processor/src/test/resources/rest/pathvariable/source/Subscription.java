package rest.pathvariable;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.core.annotations.rest.Update;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Aggregate
public record Subscription(
        @Id String id,
        @Version long version,
        String plan,
        String email) {

    @Create(path = "/{plan}")
    public Subscription(String plan, String email) {
        this(UUID.randomUUID().toString(), 0L, plan, email);
    }

    @Update(path = "/{section}")
    public Subscription updateSection(String section, String email) {
        return new Subscription(id, version, plan, email);
    }
}

