package be.appify.prefab.processor;

import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.ParameterSpec;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface PrefabPlugin {
    default void writeController(ClassManifest manifest, TypeSpec.Builder builder, PrefabContext context) {
    }

    default Set<TypeName> getServiceDependencies(ClassManifest classManifest, PrefabContext context) {
        return Collections.emptySet();
    }

    default void writeService(ClassManifest manifest, TypeSpec.Builder builder, PrefabContext context) {
    }

    default void writeAdditionalFiles(List<ClassManifest> manifests, PrefabContext context) {
    }

    default void writeRepositoryAdapter(ClassManifest manifest, TypeSpec.Builder builder) {
    }

    default void writeCrudRepository(ClassManifest manifest, TypeSpec.Builder builder) {
    }

    default void writeRepository(ClassManifest manifest, TypeSpec.Builder builder) {
    }

    default void writeTestFixture(ClassManifest manifest, TypeSpec.Builder builder, PrefabContext context) {
    }

    default Optional<ParameterSpec> requestBodyParameter(VariableManifest parameter) {
        return Optional.empty();
    }

    default Optional<ParameterSpec> requestMethodParameter(VariableManifest parameter) {
        return Optional.empty();
    }

    default Optional<CodeBlock> mapRequestParameter(VariableManifest parameter) {
        return Optional.empty();
    }
}
