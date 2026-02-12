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
     * Initialize the plugin with the given context.
     *
     * @param context
     *        The PrefabContext providing access to processing environment and utilities.
     */
    default void initContext(PrefabContext context) {
    }

    /**
     * Write controller code for the given manifest.
     *
     * @param manifest
     *         The class manifest.
     * @param builder
     *         The TypeSpec builder for the controller.
     */
    default void writeController(ClassManifest manifest, TypeSpec.Builder builder) {
    }

    /**
     * Get service dependencies for the given manifest.
     *
     * @param classManifest
     *         The class manifest.
     * @return A set of TypeNames representing the service dependencies.
     */
    default Set<TypeName> getServiceDependencies(ClassManifest classManifest) {
        return Collections.emptySet();
    }

    /**
     * Write service code for the given manifest.
     *
     * @param manifest
     *         The class manifest.
     * @param builder
     *         The TypeSpec builder for the service.
     */
    default void writeService(ClassManifest manifest, TypeSpec.Builder builder) {
    }

    /**
     * Write additional files based on the given manifests.
     *
     * @param manifests
     *         The list of class manifests.
     */
    default void writeAdditionalFiles(List<ClassManifest> manifests) {
    }

    /**
     * Write repository code for the given manifest.
     *
     * @param manifest
     *         The class manifest.
     * @param builder
     *         The TypeSpec builder for the repository.
     */
    default void writeRepository(ClassManifest manifest, TypeSpec.Builder builder) {
    }

    /**
     * Write test REST client code for the given manifest.
     *
     * @param manifest
     *         The class manifest.
     * @param builder
     *         The TypeSpec builder for the test client.
     */
    default void writeTestClient(ClassManifest manifest, TypeSpec.Builder builder) {
    }

    /**
     * Optionally adds a request body parameter for the given variable.
     *
     * @param parameter
     *         The variable manifest.
     * @return An optional ParameterSpec for the request body.
     */
    default Optional<ParameterSpec> requestBodyParameter(VariableManifest parameter) {
        return Optional.empty();
    }

    /**
     * Optionally adds a request method parameter for the given variable.
     *
     * @param parameter
     *         The variable manifest.
     * @return An optional ParameterSpec for the request method.
     */
    default Optional<ParameterSpec> requestMethodParameter(VariableManifest parameter) {
        return Optional.empty();
    }

    /**
     * Optionally maps a request parameter for the given variable.
     *
     * @param parameter
     *         The variable manifest.
     * @return An optional CodeBlock for mapping the request parameter.
     */
    default Optional<CodeBlock> mapRequestParameter(VariableManifest parameter) {
        return Optional.empty();
    }
}
