package rest.enumfilter;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.core.annotations.rest.Filter;
import be.appify.prefab.core.annotations.rest.GetById;
import be.appify.prefab.core.annotations.rest.GetList;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Aggregate
@GetById
@GetList
public record Task(
        @Id String id,
        @Version long version,
        String title,
        @Filter TaskStatus status) {

    @Create
    public Task(String title, TaskStatus status) {
        this(UUID.randomUUID().toString(), 0L, title, status);
    }
}

