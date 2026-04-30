package be.appify.prefab.processor.event.handler;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.ByReference;
import be.appify.prefab.core.annotations.EventHandler;
import be.appify.prefab.core.annotations.Multicast;
import be.appify.prefab.processor.ClassManifest;
import be.appify.prefab.processor.PrefabContext;
import be.appify.prefab.processor.TypeManifest;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;
import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeMirror;
import org.springframework.stereotype.Component;

import static javax.lang.model.type.TypeKind.VOID;

/**
 * Prefab plugin to generate static event handlers based on EventHandler annotations.
 */
public class StaticEventHandlerPlugin implements EventHandlerPlugin {
    private final StaticEventHandlerWriter staticEventHandlerWriter = new StaticEventHandlerWriter();
    private PrefabContext context;


    @Override
    public void initContext(PrefabContext context) {
        this.context = context;
    }

    @Override
    public void writeService(ClassManifest manifest, TypeSpec.Builder builder) {
        Stream.concat(ownEventHandlers(manifest), mergedComponentHandlers(manifest))
                .forEach(handler -> builder.addMethod(
                        staticEventHandlerWriter.staticDomainHandlerMethod(manifest, handler)));
    }

    @Override
    public Set<TypeName> getServiceDependencies(ClassManifest manifest) {
        return mergedComponentHandlers(manifest)
                .map(handler -> handler.instanceMethod()
                        ? handler.componentType().asTypeName()
                        : ClassName.get(
                                handler.componentType().packageName() + ".application",
                                handler.componentType().simpleName() + "Repository"))
                .collect(Collectors.toSet());
    }

    @Override
    public void writeAdditionalFiles(List<ClassManifest> manifests) {
        validateMergedHandlers();
    }

    private void validateMergedHandlers() {
        context.roundEnvironment().getElementsAnnotatedWith(EventHandler.class)
                .stream()
                .filter(element -> element.getKind() == ElementKind.METHOD)
                .map(ExecutableElement.class::cast)
                .forEach(element -> {
                    var annotation = element.getAnnotationsByType(EventHandler.class)[0];
                    var mirror = getAnnotationValueMirror(annotation);
                    if (mirror == null) {
                        return;
                    }
                    var aggregateType = TypeManifest.of(mirror, context.processingEnvironment());
                    if (aggregateType.annotationsOfType(Aggregate.class).isEmpty()) {
                        context.logError(
                                "@EventHandler value %s must be annotated with @Aggregate".formatted(
                                        aggregateType.simpleName()),
                                element);
                    }
                });
    }

    private Stream<StaticEventHandlerManifest> ownEventHandlers(ClassManifest manifest) {
        var typeElement = manifest.type().asElement();
        return typeElement.getEnclosedElements()
                .stream()
                .filter(element -> element.getKind() == ElementKind.METHOD
                        && element.getModifiers().containsAll(Set.of(Modifier.PUBLIC, Modifier.STATIC)))
                .map(ExecutableElement.class::cast)
                .filter(element -> element.getAnnotationsByType(EventHandler.class).length > 0)
                .flatMap(element -> {
                    var annotation = element.getAnnotationsByType(EventHandler.class)[0];
                    if (getAnnotationValueMirror(annotation) != null) {
                        return Stream.empty();
                    }
                    if (element.getReturnType().getKind() == VOID) {
                        context.logError(
                                "Domain event handler method %s must return either %s or Optional<%s>".formatted(
                                        element,
                                        typeElement, typeElement),
                                element);
                        return Stream.empty();
                    }
                    var returnType = TypeManifest.of(element.getReturnType(), context.processingEnvironment());
                    if (returnType.is(Optional.class)) {
                        returnType = returnType.parameters().getFirst();
                    }
                    if (!Objects.equals(returnType.asElement(), typeElement)) {
                        context.logError(
                                "Domain event handler method %s must return either %s or Optional<%s>".formatted(
                                        element,
                                        typeElement, typeElement),
                                element);
                        return Stream.empty();
                    }
                    var parameters = element.getParameters();
                    if (parameters.size() != 1) {
                        context.logError(
                                "Domain event handler method %s must have exactly one parameter".formatted(element),
                                element
                        );
                        return Stream.empty();
                    }
                    var eventType = TypeManifest.of(parameters.getFirst().asType(), context.processingEnvironment());
                    if (hasByReferenceCompanion(typeElement, eventType)) {
                        return Stream.empty();
                    }
                    if (hasMulticastCompanion(typeElement, eventType)) {
                        return Stream.empty();
                    }
                    return Stream.of(StaticEventHandlerManifest.ofOwnHandler(
                            element.getSimpleName().toString(),
                            eventType,
                            TypeManifest.of(element.getReturnType(), context.processingEnvironment())));
                });
    }

    private boolean hasByReferenceCompanion(TypeElement typeElement, TypeManifest eventType) {
        return hasInstanceCompanion(typeElement, eventType, ByReference.class);
    }

    private boolean hasMulticastCompanion(TypeElement typeElement, TypeManifest eventType) {
        return hasInstanceCompanion(typeElement, eventType, Multicast.class);
    }

    private boolean hasInstanceCompanion(TypeElement typeElement, TypeManifest eventType,
            Class<? extends java.lang.annotation.Annotation> annotation) {
        return typeElement.getEnclosedElements()
                .stream()
                .filter(element -> element.getKind() == ElementKind.METHOD
                        && element.getModifiers().contains(Modifier.PUBLIC)
                        && !element.getModifiers().contains(Modifier.STATIC))
                .map(ExecutableElement.class::cast)
                .filter(element -> element.getAnnotationsByType(annotation).length > 0)
                .filter(element -> element.getParameters().size() == 1)
                .anyMatch(element -> {
                    var paramType = TypeManifest.of(element.getParameters().getFirst().asType(),
                            context.processingEnvironment());
                    return paramType.asElement() != null
                            && paramType.asElement().equals(eventType.asElement());
                });
    }

    private Stream<StaticEventHandlerManifest> mergedComponentHandlers(ClassManifest manifest) {
        return context.roundEnvironment().getElementsAnnotatedWith(EventHandler.class)
                .stream()
                .filter(element -> element.getKind() == ElementKind.METHOD)
                .map(ExecutableElement.class::cast)
                .filter(element -> {
                    var annotation = element.getAnnotationsByType(EventHandler.class)[0];
                    var mirror = getAnnotationValueMirror(annotation);
                    if (mirror == null) {
                        return false;
                    }
                    var aggregateType = TypeManifest.of(mirror, context.processingEnvironment());
                    return Objects.equals(aggregateType.asElement(), manifest.type().asElement());
                })
                .flatMap(element -> buildMergedManifest(element).stream());
    }

    private Optional<StaticEventHandlerManifest> buildMergedManifest(ExecutableElement element) {
        var annotation = element.getAnnotationsByType(EventHandler.class)[0];
        var aggregateTypeMirror = Objects.requireNonNull(getAnnotationValueMirror(annotation));
        var aggregateType = TypeManifest.of(aggregateTypeMirror, context.processingEnvironment());

        if (aggregateType.annotationsOfType(Aggregate.class).isEmpty()) {
            context.logError(
                    "@EventHandler value %s must be annotated with @Aggregate".formatted(
                            aggregateType.simpleName()),
                    element);
            return Optional.empty();
        }

        boolean isStatic = element.getModifiers().containsAll(Set.of(Modifier.PUBLIC, Modifier.STATIC));
        boolean isPublicInstance = element.getModifiers().contains(Modifier.PUBLIC)
                && !element.getModifiers().contains(Modifier.STATIC);

        var componentElement = (TypeElement) element.getEnclosingElement();
        var componentType = TypeManifest.of(componentElement.asType(), context.processingEnvironment());

        if (isPublicInstance) {
            return buildMergedInstanceHandlerManifest(element, componentType);
        }

        if (!isStatic) {
            context.logError(
                    "Merged @EventHandler method %s must be public and static, or a public instance method on a @Component".formatted(element),
                    element);
            return Optional.empty();
        }

        // Static merged handler
        if (element.getReturnType().getKind() == VOID) {
            context.logError(
                    "Merged @EventHandler method %s must return %s or Optional<%s>".formatted(
                            element, componentType.simpleName(), componentType.simpleName()),
                    element);
            return Optional.empty();
        }

        var returnType = TypeManifest.of(element.getReturnType(), context.processingEnvironment());
        var actualReturnType = returnType.is(Optional.class) ? returnType.parameters().getFirst() : returnType;
        if (!Objects.equals(actualReturnType.asElement(), componentElement)) {
            context.logError(
                    "Merged @EventHandler method %s must return %s or Optional<%s>".formatted(
                            element, componentType.simpleName(), componentType.simpleName()),
                    element);
            return Optional.empty();
        }

        var parameters = element.getParameters();
        if (parameters.size() != 1) {
            context.logError(
                    "Merged @EventHandler method %s must have exactly one parameter".formatted(element),
                    element);
            return Optional.empty();
        }

        var eventType = TypeManifest.of(parameters.getFirst().asType(), context.processingEnvironment());
        return Optional.of(StaticEventHandlerManifest.ofMergedStaticHandler(
                element.getSimpleName().toString(),
                eventType,
                TypeManifest.of(element.getReturnType(), context.processingEnvironment()),
                componentType));
    }

    private Optional<StaticEventHandlerManifest> buildMergedInstanceHandlerManifest(
            ExecutableElement element, TypeManifest componentType) {
        if (componentType.annotationsOfType(Component.class).isEmpty()) {
            context.logError(
                    "Merged @EventHandler instance method %s must be on a class annotated with @Component".formatted(
                            element.getSimpleName()),
                    element);
            return Optional.empty();
        }

        var parameters = element.getParameters();
        if (parameters.size() != 1) {
            context.logError(
                    "Merged @EventHandler method %s must have exactly one parameter".formatted(
                            element.getSimpleName()),
                    element);
            return Optional.empty();
        }

        var eventType = TypeManifest.of(parameters.getFirst().asType(), context.processingEnvironment());
        return Optional.of(StaticEventHandlerManifest.ofMergedInstanceHandler(
                element.getSimpleName().toString(),
                eventType,
                null,
                componentType));
    }

    /**
     * Returns the {@link TypeMirror} for {@link EventHandler#value()}, or {@code null} when the value is the default
     * ({@code void.class}), indicating that no aggregate root merge was requested.
     */
    private TypeMirror getAnnotationValueMirror(EventHandler annotation) {
        try {
            var value = annotation.value();
            if (value == void.class) {
                return null;
            }
            TypeElement typeElement =
                    context.processingEnvironment().getElementUtils().getTypeElement(value.getCanonicalName());
            return typeElement != null ? typeElement.asType() : null;
        } catch (MirroredTypeException e) {
            var mirror = e.getTypeMirror();
            return mirror.getKind() == javax.lang.model.type.TypeKind.VOID ? null : mirror;
        }
    }

    @Override
    public Class<? extends Annotation> annotation() {
        return EventHandler.class;
    }
}
