package be.appify.prefab.core.evolution;

import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.deser.std.DelegatingDeserializer;
import tools.jackson.databind.node.ObjectNode;

/** Applies the evolution declarations of a record to the message before it is bound to the record. */
final class EvolvingDeserializer extends DelegatingDeserializer {
    private final RecordEvolution evolution;

    EvolvingDeserializer(ValueDeserializer<?> delegate, RecordEvolution evolution) {
        super(delegate);
        this.evolution = evolution;
    }

    @Override
    protected ValueDeserializer<?> newDelegatingInstance(ValueDeserializer<?> newDelegatee) {
        return new EvolvingDeserializer(newDelegatee, evolution);
    }

    @Override
    public Object deserialize(JsonParser parser, DeserializationContext context) {
        var message = context.readTree(parser);
        if (message instanceof ObjectNode object) {
            evolution.apply(object);
        }
        try (var treeParser = message.traverse(context)) {
            treeParser.nextToken();
            return _delegatee.deserialize(treeParser, context);
        }
    }
}
