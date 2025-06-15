package be.appify.prefab.processor.eventhandler.broadcast;

import be.appify.prefab.core.annotations.EventHandler;
import be.appify.prefab.processor.ClassManifest;
import be.appify.prefab.processor.PrefabContext;
import be.appify.prefab.processor.PrefabPlugin;
import be.appify.prefab.processor.TypeManifest;
import com.palantir.javapoet.TypeSpec;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import java.util.stream.Stream;

public class BroadcastEventHandlerPlugin implements PrefabPlugin {
    private final BroadcastEventHandlerWriter broadcastEventHandlerWriter = new BroadcastEventHandlerWriter();

    @Override
    public void writeService(ClassManifest manifest, TypeSpec.Builder builder, PrefabContext context) {
        broadcastEventHandlers(manifest)
                .forEach(handler ->
                        builder.addMethod(broadcastEventHandlerWriter.broadcastEventHandlerMethod(manifest, handler)));
    }

    private Stream<BroadcastEventHandlerManifest> broadcastEventHandlers(ClassManifest manifest) {
        var typeElement = manifest.type().asElement();
        return typeElement.getEnclosedElements()
                .stream()

                .filter(element -> element.getKind() == ElementKind.METHOD
                        && element.getModifiers().contains(Modifier.PUBLIC))
                .map(ExecutableElement.class::cast)
                .filter(element -> element.getAnnotationsByType(EventHandler.Broadcast.class).length > 0)
                .map(element -> {
                    var eventType = getEventType(element, manifest.processingEnvironment());
                    return new BroadcastEventHandlerManifest(
                            element.getSimpleName().toString(),
                            eventType);
                });
    }

    private TypeManifest getEventType(ExecutableElement element, ProcessingEnvironment processingEnvironment) {
        var parameters = element.getParameters();
        if (parameters.size() != 1) {
            throw new IllegalArgumentException(
                    "Domain event handler method %s must have exactly one parameter".formatted(element));
        }
        var eventType = new TypeManifest(parameters.getFirst().asType(), processingEnvironment);
        if (eventType.asElement() == null) {
            throw new IllegalArgumentException(
                    "Domain event handler method %s must have a parameter that is a declared class or record".formatted(
                            element));
        }
        return eventType;
    }
}
