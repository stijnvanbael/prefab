package be.appify.prefab.processor.eventhandler;

import be.appify.prefab.core.annotations.EventHandler;
import be.appify.prefab.processor.ClassManifest;
import be.appify.prefab.processor.PrefabContext;
import be.appify.prefab.processor.PrefabPlugin;
import be.appify.prefab.processor.TypeManifest;
import com.palantir.javapoet.TypeSpec;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

public class StaticEventHandlerPlugin implements PrefabPlugin {
    private final StaticEventHandlerWriter staticEventHandlerWriter = new StaticEventHandlerWriter();

    @Override
    public void writeService(ClassManifest manifest, TypeSpec.Builder builder, PrefabContext context) {
        staticDomainHandlers(manifest)
                .forEach(handler -> builder.addMethod(staticEventHandlerWriter.staticDomainHandlerMethod(manifest, handler)));
    }

    private Stream<StaticEventHandlerManifest> staticDomainHandlers(ClassManifest manifest) {
        var typeElement = manifest.type().asElement();
        return typeElement.getEnclosedElements()
                .stream()
                .filter(element -> element.getKind() == ElementKind.METHOD
                        && element.getModifiers().containsAll(Set.of(Modifier.PUBLIC, Modifier.STATIC)))
                .map(ExecutableElement.class::cast)
                .filter(element -> element.getAnnotationsByType(EventHandler.class).length > 0)
                .map(element -> {
                    var returnType = new TypeManifest(element.getReturnType(), manifest.processingEnvironment());
                    if (returnType.is(Optional.class)) {
                        returnType = returnType.parameters().getFirst();
                    }
                    if (!Objects.equals(returnType.asElement(), typeElement)) {
                        throw new IllegalArgumentException(
                                "Domain event handler method %s must return either %s or Optional<%s>".formatted(
                                        element,
                                        typeElement, typeElement));
                    }
                    var parameters = element.getParameters();
                    if (parameters.size() != 1) {
                        throw new IllegalArgumentException(
                                "Domain event handler method %s must have exactly one parameter".formatted(element));
                    }
                    var eventType = new TypeManifest(parameters.getFirst().asType(), manifest.processingEnvironment());
                    return new StaticEventHandlerManifest(
                            element.getSimpleName().toString(),
                            eventType,
                            new TypeManifest(element.getReturnType(), manifest.processingEnvironment()));
                });
    }
}
