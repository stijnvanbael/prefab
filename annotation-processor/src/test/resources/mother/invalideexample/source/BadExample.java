package mother.invalideexample.source;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.Example;
import be.appify.prefab.core.annotations.rest.Create;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Aggregate
public record BadExample(
        @Id String id,
        @Version long version,
        String name) {

    @Create
    public BadExample(@Example("not-a-number") int count) {
        this(UUID.randomUUID().toString(), 0L, String.valueOf(count));
    }
}

