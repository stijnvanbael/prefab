package rest.polymorphicwithparent;

import be.appify.prefab.core.annotations.Aggregate;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Aggregate
public record Canvas(@Id String id, @Version long version, String title) {
}

