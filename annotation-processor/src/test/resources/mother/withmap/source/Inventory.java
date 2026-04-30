package mother.withmap.source;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.rest.Create;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Aggregate
public record Inventory(
        @Id String id,
        @Version long version,
        String name,
        Map<String, Integer> stock) {

    @Create
    public Inventory(String name, Map<String, Integer> stock) {
        this(UUID.randomUUID().toString(), 0L, name, stock);
    }
}
