package mother.binary;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.core.domain.Binary;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Aggregate
public record Document(
        @Id String id,
        @Version long version,
        String title,
        Binary attachment) {

    @Create
    public Document(String title, Binary attachment) {
        this(UUID.randomUUID().toString(), 0L, title, attachment);
    }
}

