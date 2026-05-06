package assertion;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.core.annotations.rest.GetById;
import be.appify.prefab.core.annotations.rest.GetList;
import be.appify.prefab.core.annotations.rest.Update;
import java.util.List;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Aggregate
@GetById
@GetList
public record Product(
        @Id String id,
        @Version long version,
        String name,
        double price,
        List<String> tags) {

    @Create
    public Product(String name, double price) {
        this(UUID.randomUUID().toString(), 0L, name, price, List.of());
    }

    @Update
    public Product update(String name, double price) {
        return new Product(id, version, name, price, tags);
    }
}
