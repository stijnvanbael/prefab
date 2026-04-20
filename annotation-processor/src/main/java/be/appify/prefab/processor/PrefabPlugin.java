package be.appify.prefab.processor;

import be.appify.prefab.processor.dbmigration.DataType;
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
     * Write additional files for polymorphic aggregate manifests. Called in addition to
     * {@link #writeAdditionalFiles(List)} so that plugins can process both regular and polymorphic aggregates
     * together.
     *
     * @param manifests
     *         The list of regular class manifests (already filtered to those with {@code @DbMigration}).
     * @param polymorphicManifests
     *         The list of polymorphic aggregate manifests.
     */
    default void writeAdditionalFiles(List<ClassManifest> manifests, List<PolymorphicAggregateManifest> polymorphicManifests) {
        writeAdditionalFiles(manifests);
    }

    /**
     * Write service code for the given polymorphic aggregate manifest.
     *
     * @param manifest
     *         The polymorphic aggregate manifest.
     * @param builder
     *         The TypeSpec builder for the service.
     */
    default void writePolymorphicService(PolymorphicAggregateManifest manifest, TypeSpec.Builder builder) {
    }

    /**
     * Write controller code for the given polymorphic aggregate manifest.
     *
     * @param manifest
     *         The polymorphic aggregate manifest.
     * @param builder
     *         The TypeSpec builder for the controller.
     */
    default void writePolymorphicController(PolymorphicAggregateManifest manifest, TypeSpec.Builder builder) {
    }

    /**
     * Get additional service dependencies for a polymorphic aggregate (beyond the parent repository).
     *
     * @param manifest
     *         The polymorphic aggregate manifest.
     * @return A set of TypeNames representing extra service dependencies.
     */
    default Set<TypeName> getPolymorphicServiceDependencies(PolymorphicAggregateManifest manifest) {
        return Collections.emptySet();
    }

    /**
     * Write test REST client code for the given polymorphic aggregate manifest.
     *
     * @param manifest
     *         The polymorphic aggregate manifest.
     * @param builder
     *         The TypeSpec builder for the test client.
     */
    default void writePolymorphicTestClient(PolymorphicAggregateManifest manifest, TypeSpec.Builder builder) {
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

    /**
     * Optionally provides a database {@link DataType} for a field whose type is annotated with
     * {@link be.appify.prefab.core.annotations.CustomType}.
     *
     * <p>If this method returns a non-empty {@code Optional}, the returned {@code DataType} is used to generate the
     * corresponding database column. If all plugins return {@code Optional.empty()}, the field is skipped and no
     * column is generated.
     *
     * @param type
     *         the {@link TypeManifest} of the custom field type
     * @return an {@code Optional} containing the SQL {@code DataType}, or empty if this plugin does not handle the
     *         type
     */
    default Optional<DataType> dataTypeOf(TypeManifest type) {
        return Optional.empty();
    }

    /**
     * Optionally provides the Avro schema {@link CodeBlock} for a field whose type is annotated with
     * {@link be.appify.prefab.core.annotations.CustomType}.
     *
     * <p>The returned {@code CodeBlock} must evaluate to an {@code org.apache.avro.Schema} at runtime.
     * If all plugins return {@code Optional.empty()}, the field is omitted from the generated Avro schema.
     *
     * @param type
     *         the {@link TypeManifest} of the custom field type
     * @return an {@code Optional} containing the {@code CodeBlock} that produces the Avro {@code Schema}, or empty if
     *         this plugin does not handle the type
     */
    default Optional<CodeBlock> avroSchemaOf(TypeManifest type) {
        return Optional.empty();
    }

    /**
     * Optionally provides a {@link CodeBlock} that serialises a value of a
     * {@link be.appify.prefab.core.annotations.CustomType}-annotated type to an Avro-compatible object.
     *
     * <p>The returned {@code CodeBlock} must produce the Avro-compatible value (e.g. a {@code String} or a
     * {@code GenericRecord}). If all plugins return {@code Optional.empty()}, the field is skipped during
     * serialisation.
     *
     * @param type
     *         the {@link TypeManifest} of the custom field type
     * @param value
     *         a {@code CodeBlock} that evaluates to the Java-side field value
     * @return an {@code Optional} containing the serialisation {@code CodeBlock}, or empty if this plugin does not
     *         handle the type
     */
    default Optional<CodeBlock> toAvroValueOf(TypeManifest type, CodeBlock value) {
        return Optional.empty();
    }

    /**
     * Optionally provides a {@link CodeBlock} that deserialises an Avro value back to a value of a
     * {@link be.appify.prefab.core.annotations.CustomType}-annotated type.
     *
     * <p>The returned {@code CodeBlock} must produce the Java-side field value. If all plugins return
     * {@code Optional.empty()}, the field is skipped during deserialisation (the field is set to {@code null}).
     *
     * @param type
     *         the {@link TypeManifest} of the custom field type
     * @param value
     *         a {@code CodeBlock} that evaluates to the raw Avro value (e.g. a {@code String} or a
     *         {@code GenericRecord})
     * @return an {@code Optional} containing the deserialisation {@code CodeBlock}, or empty if this plugin does not
     *         handle the type
     */
    default Optional<CodeBlock> fromAvroValueOf(TypeManifest type, CodeBlock value) {
        return Optional.empty();
    }
}
