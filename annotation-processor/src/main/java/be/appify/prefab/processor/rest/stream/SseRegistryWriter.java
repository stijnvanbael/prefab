package be.appify.prefab.processor.rest.stream;

import be.appify.prefab.processor.ClassManifest;
import be.appify.prefab.processor.OutputTargetFileOutput;
import be.appify.prefab.processor.PrefabContext;
import be.appify.prefab.processor.TestFileOutput;
import be.appify.prefab.core.annotations.OutputTarget;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.FieldSpec;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeSpec;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import javax.lang.model.element.Modifier;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** Generates the {@code {Aggregate}SseRegistry} component for the push model. */
class SseRegistryWriter {

    private final TestFileOutput fileWriter;

    SseRegistryWriter(PrefabContext context) {
        this.fileWriter = new OutputTargetFileOutput(context, "infrastructure.http", OutputTarget.MAIN);
    }

    /**
     * Generates a {@code @Component} SSE registry class with {@code register}, {@code findById},
     * and {@code remove} methods, backed by a {@code ConcurrentHashMap}.
     *
     * @param manifest the aggregate class manifest
     */
    void writeSseRegistry(ClassManifest manifest) {
        var registryName = sseRegistrySimpleName(manifest);
        var mapType = ParameterizedTypeName.get(
                ClassName.get(ConcurrentHashMap.class),
                ClassName.get(String.class),
                ClassName.get(SseEmitter.class));
        var emittersField = FieldSpec.builder(mapType, "emitters", Modifier.PRIVATE, Modifier.FINAL)
                .initializer("new $T()", mapType)
                .build();

        var registerMethod = MethodSpec.methodBuilder("register")
                .addModifiers(Modifier.PUBLIC)
                .addParameter(String.class, "id")
                .addParameter(SseEmitter.class, "emitter")
                .addStatement("emitters.put(id, emitter)")
                .build();

        var findByIdMethod = MethodSpec.methodBuilder("findById")
                .addModifiers(Modifier.PUBLIC)
                .addParameter(String.class, "id")
                .returns(ParameterizedTypeName.get(ClassName.get(Optional.class), ClassName.get(SseEmitter.class)))
                .addStatement("return $T.ofNullable(emitters.get(id))", Optional.class)
                .build();

        var removeMethod = MethodSpec.methodBuilder("remove")
                .addModifiers(Modifier.PUBLIC)
                .addParameter(String.class, "id")
                .addStatement("emitters.remove(id)")
                .build();

        var typeSpec = TypeSpec.classBuilder(registryName)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Component.class)
                .addField(emittersField)
                .addMethod(registerMethod)
                .addMethod(findByIdMethod)
                .addMethod(removeMethod)
                .build();

        fileWriter.writeFile(manifest.packageName(), registryName, typeSpec);
    }

    static String sseRegistrySimpleName(ClassManifest manifest) {
        return "%sSseRegistry".formatted(manifest.simpleName());
    }

    static ClassName sseRegistryClassName(ClassManifest manifest) {
        return ClassName.get(
                "%s.infrastructure.http".formatted(manifest.packageName()),
                sseRegistrySimpleName(manifest));
    }
}

