package be.appify.prefab.processor.eventhandler.multicast;

import be.appify.prefab.core.annotations.EventHandler;
import be.appify.prefab.processor.ClassManifest;
import be.appify.prefab.processor.PrefabContext;
import be.appify.prefab.processor.TypeManifest;
import be.appify.prefab.processor.eventhandler.EventHandlerPlugin;
import com.palantir.javapoet.TypeSpec;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import java.lang.annotation.Annotation;
import java.util.stream.Stream;

/**
 * Prefab plugin to generate multicast event handlers based on EventHandler.Multicast annotations.
 */
public class MulticastEventHandlerPlugin implements EventHandlerPlugin {
    private final MulticastEventHandlerWriter multicastEventHandlerWriter = new MulticastEventHandlerWriter();

    /** Constructs a new MulticastEventHandlerPlugin. */
    public MulticastEventHandlerPlugin() {
    }

    @Override
    public void writeService(ClassManifest manifest, TypeSpec.Builder builder, PrefabContext context) {
        multicastEventHandlers(manifest, context)
                .forEach(handler ->
                        builder.addMethod(
                                multicastEventHandlerWriter.multicastEventHandlerMethod(manifest, handler, context)));
    }

    private Stream<MulticastEventHandlerManifest> multicastEventHandlers(ClassManifest manifest,
            PrefabContext context) {
        var typeElement = manifest.type().asElement();
        return typeElement.getEnclosedElements()
                .stream()

                .filter(element -> element.getKind() == ElementKind.METHOD
                        && element.getModifiers().contains(Modifier.PUBLIC))
                .map(ExecutableElement.class::cast)
                .flatMap(element -> Stream.of(element.getAnnotationsByType(EventHandler.Multicast.class))
                        .map(annotation -> {
                            var eventType = getEventType(element, context);
                            return new MulticastEventHandlerManifest(
                                    element,
                                    eventType,
                                    context,
                                    annotation.queryMethod(),
                                    annotation.paramMapping()
                            );
                        }));
    }

    private TypeManifest getEventType(ExecutableElement element, PrefabContext context) {
        var parameters = element.getParameters();
        if (parameters.size() != 1) {
            context.logError(
                    "Domain event handler method %s must have exactly one parameter".formatted(element),
                    element);
        }
        var eventType = new TypeManifest(parameters.getFirst().asType(), context.processingEnvironment());
        if (eventType.asElement() == null) {
            context.logError(
                    "Domain event handler method %s must have a parameter that is a declared class or record".formatted(
                            element),
                    element);
        }
        return eventType;
    }

    @Override
    public Class<? extends Annotation> annotation() {
        return EventHandler.Multicast.class;
    }
}
