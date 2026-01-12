package kafka.multitopic;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.core.domain.PublishesEvents;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.annotation.Version;

@Aggregate
public record Sale(
        @Id String id,
        @Version long version,
        @NotNull Double total,
        @NotNull LocalDate date
) implements PublishesEvents {
    @PersistenceCreator
    public Sale {
    }

    @Create
    public Sale(@NotNull Double total) {
        this(java.util.UUID.randomUUID().toString(), 0L, total, LocalDate.now());
        publish(new Created(id, total, date));
    }

    @Event(topic = "${topic.sale.name}", platform = Event.Platform.KAFKA)
    public record Created(String id, Double total, LocalDate date) {
    }
}