package be.appify.prefab.processor.event.handler;

import be.appify.prefab.core.annotations.EventHandler;
import be.appify.prefab.processor.ClassManifest;
import be.appify.prefab.processor.PrefabContext;
import be.appify.prefab.processor.TypeManifest;
import com.palantir.javapoet.TypeSpec;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import java.lang.annotation.Annotation;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static javax.lang.model.type.TypeKind.VOID;

/**
 * Prefab plugin to generate static event handlers based on EventHandler annotations.
 */
public class StaticEventHandlerPlugin implements EventHandlerPlugin {
    private final StaticEventHandlerWriter staticEventHandlerWriter = new StaticEventHandlerWriter();

    /** Constructs a new StaticEventHandlerPlugin. */
    public StaticEventHandlerPlugin() {
    }

    @Override
    public void writeService(ClassManifest manifest, TypeSpec.Builder builder, PrefabContext context) {
        staticDomainHandlers(manifest, context)
                .forEach(handler -> builder.addMethod(
                        staticEventHandlerWriter.staticDomainHandlerMethod(manifest, handler)));
    }

    private Stream<StaticEventHandlerManifest> staticDomainHandlers(ClassManifest manifest, PrefabContext context) {
        var typeElement = manifest.type().asElement();
        return typeElement.getEnclosedElements()
                .stream()
                .filter(element -> element.getKind() == ElementKind.METHOD
                        && element.getModifiers().containsAll(Set.of(Modifier.PUBLIC, Modifier.STATIC)))
                .map(ExecutableElement.class::cast)
                .filter(element -> element.getAnnotationsByType(EventHandler.class).length > 0)
                .map(element -> {
                    if (element.getReturnType().getKind() == VOID) {
                        context.logError(
                                "Domain event handler method %s must return either %s or Optional<%s>".formatted(
                                        element,
                                        typeElement, typeElement),
                                element);
                    }
                    var returnType = new TypeManifest(element.getReturnType(), context.processingEnvironment());
                    if (returnType.is(Optional.class)) {
                        returnType = returnType.parameters().getFirst();
                    }
                    if (!Objects.equals(returnType.asElement(), typeElement)) {
                        context.logError(
                                "Domain event handler method %s must return either %s or Optional<%s>".formatted(
                                        element,
                                        typeElement, typeElement),
                                element);
                    }
                    var parameters = element.getParameters();
                    if (parameters.size() != 1) {
                        context.logError(
                                "Domain event handler method %s must have exactly one parameter".formatted(element),
                                element
                        );
                    }
                    var eventType = new TypeManifest(parameters.getFirst().asType(), context.processingEnvironment());
                    return new StaticEventHandlerManifest(
                            element.getSimpleName().toString(),
                            eventType,
                            new TypeManifest(element.getReturnType(), context.processingEnvironment()));
                });
    }

    @Override
    public Class<? extends Annotation> annotation() {
        return EventHandler.class;
    }
}
