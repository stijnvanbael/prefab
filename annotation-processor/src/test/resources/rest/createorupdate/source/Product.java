package rest.createorupdate;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.core.annotations.rest.HttpMethod;
import be.appify.prefab.core.annotations.rest.Update;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Aggregate
public record Product(
        @Id String id,
        @Version long version,
        String name,
        String price
) {
    @Create(method = HttpMethod.PUT, path = "/{id}")
    public Product(String id, String name, String price) {
        this(id, 0L, name, price);
    }

    @Update(method = HttpMethod.PUT)
    public Product update(String name, String price) {
        return new Product(id, version, name, price);
    }
}
