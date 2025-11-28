package be.appify.prefab.processor.eventhandler.broadcast;

import be.appify.prefab.core.annotations.EventHandler;
import be.appify.prefab.processor.ClassManifest;
import be.appify.prefab.processor.PrefabContext;
import be.appify.prefab.processor.PrefabPlugin;
import be.appify.prefab.processor.TypeManifest;
import com.palantir.javapoet.TypeSpec;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import java.util.stream.Stream;

public class BroadcastEventHandlerPlugin implements PrefabPlugin {
    private final BroadcastEventHandlerWriter broadcastEventHandlerWriter = new BroadcastEventHandlerWriter();

    @Override
    public void writeService(ClassManifest manifest, TypeSpec.Builder builder, PrefabContext context) {
        broadcastEventHandlers(manifest, context)
                .forEach(handler ->
                        builder.addMethod(broadcastEventHandlerWriter.broadcastEventHandlerMethod(manifest, handler)));
    }

    private Stream<BroadcastEventHandlerManifest> broadcastEventHandlers(ClassManifest manifest,
            PrefabContext context) {
        var typeElement = manifest.type().asElement();
        return typeElement.getEnclosedElements()
                .stream()

                .filter(element -> element.getKind() == ElementKind.METHOD
                        && element.getModifiers().contains(Modifier.PUBLIC))
                .map(ExecutableElement.class::cast)
                .filter(element -> element.getAnnotationsByType(EventHandler.Broadcast.class).length > 0)
                .map(element -> {
                    var eventType = getEventType(element, context);
                    return new BroadcastEventHandlerManifest(
                            element.getSimpleName().toString(),
                            eventType,
                            new TypeManifest(element.getReturnType(), context.processingEnvironment()));
                });
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
}
