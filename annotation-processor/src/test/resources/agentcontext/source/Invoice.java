package agentcontext.source;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.EventHandler;
import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.core.annotations.rest.GetById;
import org.springframework.data.annotation.Id;

@Aggregate
@GetById
public record Invoice(@Id String id, int orders) {

    @Create
    public Invoice() {
        this(java.util.UUID.randomUUID().toString(), 0);
    }

    @EventHandler
    public static Invoice on(OrderEvent.Placed event) {
        return new Invoice();
    }
}
