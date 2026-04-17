package mother.person;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.Doc;
import be.appify.prefab.core.annotations.Example;
import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.core.annotations.rest.GetById;
import be.appify.prefab.core.annotations.rest.Update;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Aggregate
@GetById
public record Person(
        @Id String id,
        @Version long version,
        @Example("Alice") @Doc("Full name of the person") String name,
        @Example("alice@example.com") String email) {

    @Create
    public Person(
            @Example("Alice") @Doc("Full name of the person") String name,
            @Example("alice@example.com") String email) {
        this(UUID.randomUUID().toString(), 0L, name, email);
    }

    @Update
    public Person update(
            @Example("Bob") @Doc("Full name of the person") String name,
            @Example("bob@example.com") String email) {
        return new Person(id, version, name, email);
    }
}
