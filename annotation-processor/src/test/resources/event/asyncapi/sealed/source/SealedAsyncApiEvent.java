package event.asyncapi;

import be.appify.prefab.core.annotations.Event;

@Event(topic = "sealed-events")
public sealed interface SealedAsyncApiEvent permits SealedAsyncApiEvent.Created, SealedAsyncApiEvent.Updated {
    record Created(String name) implements SealedAsyncApiEvent {}
    record Updated(String name) implements SealedAsyncApiEvent {}
}
