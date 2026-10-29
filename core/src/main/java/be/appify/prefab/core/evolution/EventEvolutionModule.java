package be.appify.prefab.core.evolution;

import tools.jackson.databind.DeserializationConfig;
import tools.jackson.databind.BeanDescription;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.deser.ValueDeserializerModifier;
import tools.jackson.databind.module.SimpleModule;

/**
 * Jackson module that makes JSON deserialization honour the evolution annotations of the event contract:
 * {@link be.appify.prefab.core.annotations.PreviousName} on fields and event types, and
 * {@link be.appify.prefab.core.annotations.Default} on fields. Types without such annotations are not affected.
 */
public class EventEvolutionModule extends SimpleModule {

    /** Creates the module. */
    public EventEvolutionModule() {
        super("PrefabEventEvolutionModule");
    }

    @Override
    public void setupModule(SetupContext context) {
        super.setupModule(context);
        context.addDeserializerModifier(new EvolvingRecordModifier());
        context.addHandler(new PreviousTypeIdHandler());
    }

    private static final class EvolvingRecordModifier extends ValueDeserializerModifier {
        @Override
        public ValueDeserializer<?> modifyDeserializer(
                DeserializationConfig config,
                BeanDescription.Supplier beanDescription,
                ValueDeserializer<?> deserializer
        ) {
            return RecordEvolution.of(beanDescription.getBeanClass())
                    .<ValueDeserializer<?>>map(evolution -> new EvolvingDeserializer(deserializer, evolution))
                    .orElse(deserializer);
        }
    }
}
