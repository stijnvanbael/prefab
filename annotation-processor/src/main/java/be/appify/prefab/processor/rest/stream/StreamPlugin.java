package be.appify.prefab.processor.rest.stream;

import be.appify.prefab.core.annotations.ByReference;
import be.appify.prefab.core.annotations.EventHandler;
import be.appify.prefab.core.annotations.rest.Streaming;
import be.appify.prefab.processor.ClassManifest;
import be.appify.prefab.processor.PrefabContext;
import be.appify.prefab.processor.PrefabPlugin;
import be.appify.prefab.processor.TypeManifest;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.type.TypeKind;

/**
 * Prefab plugin that generates Server-Sent Events (SSE) endpoints for methods annotated with
 * {@link Streaming}.
 *
 * <p>Two models are supported:
 * <ul>
 *   <li><strong>Pull model</strong>: an instance method on the aggregate returning
 *       {@code java.util.stream.Stream<T>} or {@code reactor.core.publisher.Flux<T>}.</li>
 *   <li><strong>Push model</strong>: an instance {@code @EventHandler @ByReference} method also
 *       annotated with {@code @Streaming}.</li>
 * </ul>
 */
public class StreamPlugin implements PrefabPlugin {

    private static final String JAVA_STREAM_CLASS = "java.util.stream.Stream";
    private static final String FLUX_CLASS = "reactor.core.publisher.Flux";

    private PrefabContext context;
    private StreamControllerWriter controllerWriter;
    private StreamServiceWriter serviceWriter;
    private SseRegistryWriter registryWriter;

    /** Creates a new instance of StreamPlugin. */
    public StreamPlugin() {
    }

    @Override
    public void initContext(PrefabContext context) {
        this.context = context;
        this.controllerWriter = new StreamControllerWriter();
        this.serviceWriter = new StreamServiceWriter(context);
        this.registryWriter = new SseRegistryWriter(context);
    }

    @Override
    public void writeController(ClassManifest manifest, TypeSpec.Builder builder) {
        pullStreamMethods(manifest).forEach(method -> {
            var annotation = method.getAnnotation(Streaming.class);
            var returnType = TypeManifest.of(method.getReturnType(), context.processingEnvironment());
            var elementType = returnType.parameters().isEmpty() ? returnType : returnType.parameters().getFirst();
            if (isJavaStream(returnType)) {
                controllerWriter.addPullStreamEndpoint(manifest, builder, method, annotation, elementType);
            } else if (isFlux(returnType)) {
                controllerWriter.addPullFluxEndpoint(manifest, builder, method, annotation, elementType);
            }
        });

        if (!serviceWriter.pushStreamMethods(manifest).isEmpty()) {
            var annotation = serviceWriter.pushStreamMethods(manifest).getFirst().getAnnotation(Streaming.class);
            controllerWriter.addPushStreamEndpoint(manifest, builder, annotation);
        }
    }

    @Override
    public void writeService(ClassManifest manifest, TypeSpec.Builder builder) {
        serviceWriter.writePushServiceMethods(manifest, builder);
    }

    @Override
    public Set<TypeName> getServiceDependencies(ClassManifest manifest) {
        if (!serviceWriter.pushStreamMethods(manifest).isEmpty()) {
            return Set.of(SseRegistryWriter.sseRegistryClassName(manifest));
        }
        return Collections.emptySet();
    }

    @Override
    public void writeAdditionalFiles(List<ClassManifest> manifests) {
        manifests.stream()
                .filter(manifest -> !serviceWriter.pushStreamMethods(manifest).isEmpty())
                .forEach(registryWriter::writeSseRegistry);
    }

    /**
     * Returns all pull-model {@code @Streaming} methods: instance methods on the aggregate that
     * are NOT also annotated with {@code @EventHandler} or {@code @ByReference}, and return
     * {@code java.util.stream.Stream<T>} or {@code Flux<T>}.
     *
     * @param manifest the aggregate manifest
     * @return list of pull-model stream methods
     */
    private List<ExecutableElement> pullStreamMethods(ClassManifest manifest) {
        return manifest.type().asElement().getEnclosedElements().stream()
                .filter(e -> e.getKind() == ElementKind.METHOD)
                .filter(e -> e.getModifiers().contains(Modifier.PUBLIC))
                .filter(e -> !e.getModifiers().contains(Modifier.STATIC))
                .map(ExecutableElement.class::cast)
                .filter(e -> e.getAnnotation(Streaming.class) != null)
                .filter(e -> e.getAnnotation(EventHandler.class) == null)
                .filter(e -> e.getAnnotation(ByReference.class) == null)
                .filter(e -> e.getReturnType().getKind() != TypeKind.VOID)
                .filter(e -> {
                    var returnType = TypeManifest.of(e.getReturnType(), context.processingEnvironment());
                    return isJavaStream(returnType) || isFlux(returnType);
                })
                .toList();
    }

    private boolean isJavaStream(TypeManifest type) {
        return JAVA_STREAM_CLASS.equals(type.packageName() + "." + type.simpleName())
                || "Stream".equals(type.simpleName()) && "java.util.stream".equals(type.packageName());
    }

    private boolean isFlux(TypeManifest type) {
        return FLUX_CLASS.equals(type.packageName() + "." + type.simpleName())
                || "Flux".equals(type.simpleName()) && "reactor.core.publisher".equals(type.packageName());
    }
}

