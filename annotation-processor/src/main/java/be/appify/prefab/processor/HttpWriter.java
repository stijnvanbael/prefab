package be.appify.prefab.processor;

import be.appify.prefab.processor.rest.ControllerUtil;
import com.palantir.javapoet.AnnotationSpec;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterSpec;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeSpec;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;
import java.util.stream.Collectors;

import static be.appify.prefab.processor.rest.ControllerUtil.responseType;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;

class HttpWriter {
    private final JavaFileWriter fileWriter;
    private final PrefabContext context;

    HttpWriter(PrefabContext context) {
        this.context = context;
        fileWriter = new JavaFileWriter(context.processingEnvironment(), "infrastructure.http");
    }

    void writeHttpLayer(ClassManifest manifest) {
        writeController(manifest);
        writeResponseRecord(manifest);
    }

    private void writeController(ClassManifest manifest) {
        var serviceType = ClassName.get("%s.application".formatted(manifest.packageName()),
                "%sService".formatted(manifest.simpleName()));
        var type = TypeSpec.classBuilder("%sController".formatted(manifest.simpleName()))
                .addModifiers(PUBLIC)
                .addAnnotation(RestController.class)
                .addAnnotation(AnnotationSpec.builder(RequestMapping.class)
                        .addMember("path", "$S", ControllerUtil.pathOf(manifest))
                        .build())
                .addField(serviceType, "service", PRIVATE, FINAL)
                .addMethod(MethodSpec.constructorBuilder()
                        .addParameter(serviceType, "service")
                        .addStatement("this.service = service")
                        .build())
                .addMethod(toResponseMethod(manifest));
        context.plugins().forEach(plugin -> plugin.writeController(manifest, type, context));
        fileWriter.writeFile(manifest.packageName(), "%sController".formatted(manifest.simpleName()), type.build());
    }

    private MethodSpec toResponseMethod(ClassManifest manifest) {
        return MethodSpec.methodBuilder("toResponse")
                .addModifiers(PRIVATE, STATIC)
                .returns(ParameterizedTypeName.get(ClassName.get(ResponseEntity.class),
                        responseType(manifest)))
                .addParameter(ParameterizedTypeName.get(ClassName.get(Optional.class),
                                ClassName.get(manifest.packageName(), manifest.simpleName())),
                        "aggregateRoot")
                .addStatement("""
                                return aggregateRoot
                                    .map($T::from)
                                    .map(ResponseEntity::ok)
                                    .orElse(ResponseEntity.notFound().build())""".stripIndent(),
                        responseType(manifest))
                .build();
    }

    private void writeResponseRecord(ClassManifest manifest) {
        var type = TypeSpec.recordBuilder("%sResponse".formatted(manifest.simpleName()))
                .addModifiers(PUBLIC)
                .recordConstructor(MethodSpec.compactConstructorBuilder()
                        .addParameters(manifest.fields().stream()
                                .map(field -> ParameterSpec.builder(
                                        field.type().asTypeName(),
                                        field.name()
                                ).build()).toList())
                        .build())
                .addMethod(MethodSpec.methodBuilder("from")
                        .addModifiers(PUBLIC, STATIC)
                        .returns(responseType(manifest))
                        .addParameter(ParameterSpec.builder(
                                ClassName.get(manifest.packageName(), manifest.simpleName()),
                                "aggregateRoot"
                        ).build())
                        .addStatement("return new $T($L)",
                                responseType(manifest),
                                manifest.fields().stream()
                                        .map(field -> "aggregateRoot.%s()".formatted(field.name()))
                                        .collect(Collectors.joining(",\n"))
                        )
                        .build())
                .build();
        fileWriter.writeFile(manifest.packageName(), "%sResponse".formatted(manifest.simpleName()), type);
    }
}