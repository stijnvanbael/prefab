package be.appify.prefab.processor;

import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.ParameterSpec;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Interface for Prefab plugins to extend functionality. */
public interface PrefabPlugin {
    /**
     * Write controller code for the given manifest.
     * @param manifest The class manifest.
     * @param builder  The TypeSpec builder for the controller.
     * @param context  The Prefab context.
     */
    default void writeController(ClassManifest manifest, TypeSpec.Builder builder, PrefabContext context) {
    }

    /**
     * Get service dependencies for the given manifest.
     * @param classManifest The class manifest.
     * @param context       The Prefab context.
     * @return A set of TypeNames representing the service dependencies.
     */
    default Set<TypeName> getServiceDependencies(ClassManifest classManifest, PrefabContext context) {
        return Collections.emptySet();
    }

    /**
     * Write service code for the given manifest.
     * @param manifest The class manifest.
     * @param builder  The TypeSpec builder for the service.
     * @param context  The Prefab context.
     */
    default void writeService(ClassManifest manifest, TypeSpec.Builder builder, PrefabContext context) {
    }

    /**
     * Write additional files based on the given manifests.
     * @param manifests The list of class manifests.
     * @param context   The Prefab context.
     */
    default void writeAdditionalFiles(List<ClassManifest> manifests, PrefabContext context) {
    }

    /**
     * Write CRUD repository code for the given manifest.
     * @param manifest The class manifest.
     * @param builder  The TypeSpec builder for the repository.
     */
    default void writeCrudRepository(ClassManifest manifest, TypeSpec.Builder builder) {
    }

    /**
     * Write test fixture code for the given manifest.
     * @param manifest The class manifest.
     * @param builder  The TypeSpec builder for the test fixture.
     * @param context  The Prefab context.
     */
    default void writeTestFixture(ClassManifest manifest, TypeSpec.Builder builder, PrefabContext context) {
    }

    /**
     * Optionally adds a request body parameter for the given variable.
     * @param parameter The variable manifest.
     * @return An optional ParameterSpec for the request body.
     */
    default Optional<ParameterSpec> requestBodyParameter(VariableManifest parameter) {
        return Optional.empty();
    }

    /**
     * Optionally adds a request method parameter for the given variable.
     * @param parameter The variable manifest.
     * @return An optional ParameterSpec for the request method.
     */
    default Optional<ParameterSpec> requestMethodParameter(VariableManifest parameter) {
        return Optional.empty();
    }

    /**
     * Optionally maps a request parameter for the given variable.
     * @param parameter The variable manifest.
     * @return An optional CodeBlock for mapping the request parameter.
     */
    default Optional<CodeBlock> mapRequestParameter(VariableManifest parameter) {
        return Optional.empty();
    }
}
