package be.appify.prefab.processor.binary;

import be.appify.prefab.core.domain.Binary;
import be.appify.prefab.processor.ClassManifest;
import be.appify.prefab.processor.PrefabContext;
import be.appify.prefab.processor.PrefabPlugin;
import be.appify.prefab.processor.VariableManifest;
import be.appify.prefab.processor.spring.StorageService;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.palantir.javapoet.AnnotationSpec;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.ParameterSpec;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

public class BinaryPlugin implements PrefabPlugin {
    private final BinaryControllerWriter binaryControllerWriter = new BinaryControllerWriter();
    private final BinaryTestFixtureWriter binaryTestFixtureWriter = new BinaryTestFixtureWriter();

    @Override
    public Optional<ParameterSpec> requestMethodParameter(VariableManifest parameter) {
        if (parameter.type().is(Binary.class)) {
            return Optional.of(ParameterSpec.builder(MultipartFile.class, parameter.name()).build());
        } else {
            return Optional.empty();
        }
    }

    @Override
    public Optional<ParameterSpec> requestBodyParameter(VariableManifest parameter) {
        if (parameter.type().is(Binary.class)) {
            return Optional.of(ParameterSpec.builder(MultipartFile.class, parameter.name())
                    .addAnnotation(AnnotationSpec.builder(RequestPart.class)
                            .addMember("name", "$S", parameter.name())
                            .build())
                    .addAnnotation(JsonIgnore.class)
                    .build());
        } else {
            return Optional.empty();
        }
    }

    @Override
    public Optional<CodeBlock> mapRequestParameter(VariableManifest parameter) {
        if (parameter.type().is(Binary.class)) {
            return Optional.of(CodeBlock.of("storageService.store(request.$N())", parameter.name()));
        } else {
            return Optional.empty();
        }
    }

    @Override
    public Set<TypeName> getServiceDependencies(ClassManifest classManifest, PrefabContext context) {
        if (classManifest.fields().stream().anyMatch(f -> f.type().is(Binary.class))) {
            return Set.of(ClassName.get(StorageService.class));
        }
        return Collections.emptySet();
    }

    @Override
    public void writeController(ClassManifest manifest, TypeSpec.Builder builder, PrefabContext context) {
        manifest.fields().stream()
                .filter(f -> f.type().is(Binary.class))
                .forEach(field ->
                        builder.addMethod(binaryControllerWriter.downloadMethod(manifest, field)));
    }

    @Override
    public void writeTestFixture(ClassManifest manifest, TypeSpec.Builder builder, PrefabContext context) {
        manifest.fields().stream()
                .filter(f -> f.type().is(Binary.class))
                .forEach(field ->
                        builder.addMethod(binaryTestFixtureWriter.downloadMethod(manifest, field)));
    }

}
