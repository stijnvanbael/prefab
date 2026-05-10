package be.appify.prefab.processor.rest.stream;

import be.appify.prefab.core.annotations.rest.Streaming;
import be.appify.prefab.core.problem.NotFoundException;
import be.appify.prefab.processor.ClassManifest;
import be.appify.prefab.processor.TypeManifest;
import com.palantir.javapoet.AnnotationSpec;
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.FieldSpec;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterSpec;
import com.palantir.javapoet.TypeSpec;
import java.io.IOException;
import java.time.Duration;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static be.appify.prefab.processor.rest.ControllerUtil.operationAnnotation;
import static be.appify.prefab.processor.rest.ControllerUtil.pathParameterAnnotation;
import static be.appify.prefab.processor.rest.ControllerUtil.securedAnnotation;
import static be.appify.prefab.processor.rest.stream.SseRegistryWriter.sseRegistryClassName;
import static be.appify.prefab.processor.rest.stream.SseRegistryWriter.sseRegistrySimpleName;
import static org.apache.commons.text.WordUtils.uncapitalize;

/** Writes SSE controller endpoints for the @{code @Streaming} annotation (pull and push models). */
class StreamControllerWriter {

    private static final String SCHEDULE_HEARTBEAT = "scheduleHeartbeat";

    /**
     * Adds the SSE endpoint for a pull-model @{code @Streaming} method (returns {@code Stream<T>}).
     *
     * @param manifest    the aggregate manifest
     * @param builder     the controller TypeSpec builder
     * @param method      the annotated method on the aggregate
     * @param annotation  the @{code @Streaming} annotation
     * @param elementType the stream element type
     */
    void addPullStreamEndpoint(ClassManifest manifest, TypeSpec.Builder builder,
            ExecutableElement method, Streaming annotation, TypeManifest elementType) {
        var methodName = method.getSimpleName().toString();
        var idParam = buildIdParameter(manifest);
        var methodBuilder = MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.PUBLIC)
                .returns(SseEmitter.class)
                .addParameter(idParam.build())
                .addAnnotation(buildGetMappingAnnotation(annotation.path()));
        operationAnnotation("Stream " + methodName + " for " + manifest.simpleName())
                .ifPresent(methodBuilder::addAnnotation);
        securedAnnotation(annotation.security()).ifPresent(methodBuilder::addAnnotation);

        methodBuilder.addStatement("var aggregate = service.getById(id).orElseThrow(() -> new $T(id))",
                NotFoundException.class);
        methodBuilder.addStatement("var emitter = new $T(0L)", SseEmitter.class);
        methodBuilder.addStatement(CodeBlock.of(
                """
                $T.ofVirtual().start(() -> {
                    try (var items = aggregate.$L()) {
                        items.forEach(item -> {
                            try {
                                emitter.send($T.event().name($S).data(item));
                            } catch ($T e) {
                                emitter.completeWithError(e);
                            }
                        });
                        emitter.complete();
                    } catch ($T e) {
                        emitter.completeWithError(e);
                    }
                })""",
                Thread.class, methodName, SseEmitter.class, annotation.event(),
                IOException.class, Exception.class));

        if (annotation.heartbeatSeconds() > 0) {
            methodBuilder.addStatement("$L(emitter, $L)", SCHEDULE_HEARTBEAT, annotation.heartbeatSeconds());
        }
        methodBuilder.addStatement("return emitter");
        builder.addMethod(methodBuilder.build());
        ensureHeartbeatMethod(builder);
    }

    /**
     * Adds the SSE endpoint for a pull-model @{code @Streaming} method returning {@code Flux<T>}.
     *
     * @param manifest    the aggregate manifest
     * @param builder     the controller TypeSpec builder
     * @param method      the annotated method on the aggregate
     * @param annotation  the @{code @Streaming} annotation
     * @param elementType the stream element type
     */
    void addPullFluxEndpoint(ClassManifest manifest, TypeSpec.Builder builder,
            ExecutableElement method, Streaming annotation, TypeManifest elementType) {
        var methodName = method.getSimpleName().toString();
        var idParam = buildIdParameter(manifest);
        var methodBuilder = MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.PUBLIC)
                .returns(SseEmitter.class)
                .addParameter(idParam.build())
                .addAnnotation(buildGetMappingAnnotation(annotation.path()));
        operationAnnotation("Stream " + methodName + " for " + manifest.simpleName())
                .ifPresent(methodBuilder::addAnnotation);
        securedAnnotation(annotation.security()).ifPresent(methodBuilder::addAnnotation);

        methodBuilder.addStatement("var aggregate = service.getById(id).orElseThrow(() -> new $T(id))",
                NotFoundException.class);
        methodBuilder.addStatement("var emitter = new $T(0L)", SseEmitter.class);
        methodBuilder.addStatement(CodeBlock.of(
                """
                aggregate.$L().subscribe(
                    item -> {
                        try {
                            emitter.send($T.event().name($S).data(item));
                        } catch ($T e) {
                            emitter.completeWithError(e);
                        }
                    },
                    emitter::completeWithError,
                    emitter::complete)""",
                methodName, SseEmitter.class, annotation.event(), IOException.class));

        if (annotation.heartbeatSeconds() > 0) {
            methodBuilder.addStatement("$L(emitter, $L)", SCHEDULE_HEARTBEAT, annotation.heartbeatSeconds());
        }
        methodBuilder.addStatement("return emitter");
        builder.addMethod(methodBuilder.build());
        ensureHeartbeatMethod(builder);
    }

    /**
     * Adds the SSE connect endpoint for the push model plus an {@code @Autowired} registry field.
     *
     * @param manifest   the aggregate manifest
     * @param builder    the controller TypeSpec builder
     * @param annotation the @{code @Streaming} annotation
     */
    void addPushStreamEndpoint(ClassManifest manifest, TypeSpec.Builder builder, Streaming annotation) {
        var registryClassName = sseRegistryClassName(manifest);
        var registryFieldName = uncapitalize(sseRegistrySimpleName(manifest));

        builder.addField(FieldSpec.builder(registryClassName, registryFieldName, Modifier.PRIVATE)
                .addAnnotation(Autowired.class)
                .build());

        var idParam = buildIdParameter(manifest);
        var methodBuilder = MethodSpec.methodBuilder("stream")
                .addModifiers(Modifier.PUBLIC)
                .returns(SseEmitter.class)
                .addParameter(idParam.build())
                .addAnnotation(buildGetMappingAnnotation(annotation.path()));
        operationAnnotation("Stream " + manifest.simpleName()).ifPresent(methodBuilder::addAnnotation);
        securedAnnotation(annotation.security()).ifPresent(methodBuilder::addAnnotation);

        methodBuilder.addStatement("service.getById(id).orElseThrow(() -> new $T(id))",
                NotFoundException.class);
        methodBuilder.addStatement("var emitter = new $T(0L)", SseEmitter.class);
        methodBuilder.addStatement("$L.register(id, emitter)", registryFieldName);
        methodBuilder.addStatement("emitter.onCompletion(() -> $L.remove(id))", registryFieldName);
        methodBuilder.addStatement("emitter.onTimeout(() -> $L.remove(id))", registryFieldName);
        if (annotation.heartbeatSeconds() > 0) {
            methodBuilder.addStatement("$L(emitter, $L)", SCHEDULE_HEARTBEAT, annotation.heartbeatSeconds());
        }
        methodBuilder.addStatement("return emitter");

        builder.addMethod(methodBuilder.build());
        ensureHeartbeatMethod(builder);
    }

    private AnnotationSpec buildGetMappingAnnotation(String path) {
        return AnnotationSpec.builder(GetMapping.class)
                .addMember("path", "$S", "/{id}" + path)
                .addMember("produces", "$T.TEXT_EVENT_STREAM_VALUE", MediaType.class)
                .build();
    }

    private ParameterSpec.Builder buildIdParameter(ClassManifest manifest) {
        var idParam = ParameterSpec.builder(String.class, "id")
                .addAnnotation(PathVariable.class);
        pathParameterAnnotation("The " + manifest.simpleName() + " ID").ifPresent(idParam::addAnnotation);
        return idParam;
    }

    /**
     * Adds the {@code scheduleHeartbeat} helper method to the controller builder.
     * Calling this multiple times is safe — duplicate method names will be deduplicated by JavaPoet.
     */
    private void ensureHeartbeatMethod(TypeSpec.Builder builder) {
        builder.addMethod(scheduleHeartbeatMethod());
    }

    private MethodSpec scheduleHeartbeatMethod() {
        return MethodSpec.methodBuilder(SCHEDULE_HEARTBEAT)
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                .addParameter(SseEmitter.class, "emitter")
                .addParameter(int.class, "intervalSeconds")
                .addStatement(CodeBlock.of(
                        """
                        $T.ofVirtual().start(() -> {
                            while (true) {
                                try {
                                    $T.sleep($T.ofSeconds(intervalSeconds));
                                    emitter.send($T.event().name("ping").data("{}"));
                                } catch ($T e) {
                                    $T.currentThread().interrupt();
                                    return;
                                } catch ($T | $T e) {
                                    return;
                                }
                            }
                        })""",
                        Thread.class, Thread.class, Duration.class, SseEmitter.class,
                        InterruptedException.class, Thread.class, IOException.class, IllegalStateException.class))
                .build();
    }
}

