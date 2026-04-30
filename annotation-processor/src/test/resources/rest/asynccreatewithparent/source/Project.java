package rest.asynccreatewithparent;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.service.Reference;
import org.springframework.data.annotation.Id;

@Aggregate
public record Project(
        @Id Reference<Project> id,
        String name) {
}
