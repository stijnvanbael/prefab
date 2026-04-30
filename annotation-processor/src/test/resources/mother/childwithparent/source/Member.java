package mother.childwithparent;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.core.annotations.rest.GetById;
import be.appify.prefab.core.annotations.rest.Parent;
import be.appify.prefab.core.annotations.rest.Update;
import be.appify.prefab.core.service.Reference;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Aggregate
@GetById
public record Member(
        @Id String id,
        @Version long version,
        @Parent Reference<Organisation> organisation) {

    @Create
    public Member(Reference<Organisation> organisation) {
        this(UUID.randomUUID().toString(), 0L, organisation);
    }

    @Update
    public Member move(Reference<Organisation> organisation) {
        return new Member(id, version, organisation);
    }
}

