package be.appify.prefab.processor;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.DbMigration;
import com.palantir.javapoet.TypeName;
import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;

/**
 * Manifest representing a polymorphic aggregate root defined as a sealed interface with permitted subtype records.
 *
 * <p>A polymorphic aggregate is a sealed interface annotated with {@link Aggregate} whose permitted subtypes are
 * concrete records. All subtypes share the same table in the database, discriminated by a {@code type} column.</p>
 */
public class PolymorphicAggregateManifest {

    private final TypeManifest type;
    private final List<ClassManifest> subtypes;
    private final List<VariableManifest> commonFields;
    private final List<VariableManifest> allFields;

    private PolymorphicAggregateManifest(
            TypeManifest type,
            List<ClassManifest> subtypes,
            ProcessingEnvironment processingEnvironment
    ) {
        this.type = type;
        this.subtypes = subtypes;
        this.allFields = computeAllFields();
        this.commonFields = computeCommonFields();
    }

    /**
     * Creates a PolymorphicAggregateManifest from a sealed interface TypeElement.
     *
     * @param typeElement
     *         the sealed interface type element
     * @param processingEnvironment
     *         the processing environment
     * @return the PolymorphicAggregateManifest for the given type element
     */
    public static PolymorphicAggregateManifest of(TypeElement typeElement, ProcessingEnvironment processingEnvironment) {
        var typeManifest = TypeManifest.of(typeElement.asType(), processingEnvironment);
        var subtypes = typeManifest.permittedSubtypes().stream()
                .filter(subtype -> subtype.isRecord() || subtype.asElement().getKind() == ElementKind.CLASS)
                .map(subtype -> ClassManifest.of(subtype.asElement(), processingEnvironment))
                .toList();
        return new PolymorphicAggregateManifest(typeManifest, subtypes, processingEnvironment);
    }

    private List<VariableManifest> computeAllFields() {
        return subtypes.stream()
                .flatMap(subtype -> subtype.fields().stream())
                .distinct()
                .toList();
    }

    private List<VariableManifest> computeCommonFields() {
        if (subtypes.isEmpty()) {
            return List.of();
        }
        var firstSubtypeFieldNames = subtypes.getFirst().fields().stream()
                .map(VariableManifest::name)
                .collect(Collectors.toSet());
        return subtypes.stream()
                .skip(1)
                .reduce(firstSubtypeFieldNames,
                        (commonNames, subtype) -> {
                            var subtypeNames = subtype.fields().stream()
                                    .map(VariableManifest::name)
                                    .collect(Collectors.toSet());
                            commonNames.retainAll(subtypeNames);
                            return commonNames;
                        },
                        (a, b) -> {
                            a.retainAll(b);
                            return a;
                        })
                .stream()
                .flatMap(name -> subtypes.getFirst().fields().stream()
                        .filter(field -> field.name().equals(name)))
                .toList();
    }

    /**
     * Returns the fields specific to the given subtype (not shared with all other subtypes).
     *
     * @param subtype
     *         the subtype to get specific fields for
     * @return the list of subtype-specific fields
     */
    public List<VariableManifest> subtypeSpecificFields(ClassManifest subtype) {
        var commonFieldNames = commonFields.stream()
                .map(VariableManifest::name)
                .collect(Collectors.toSet());
        return subtype.fields().stream()
                .filter(field -> !commonFieldNames.contains(field.name()))
                .toList();
    }

    /**
     * Gets the TypeManifest of the sealed interface.
     *
     * @return the type manifest
     */
    public TypeManifest type() {
        return type;
    }

    /**
     * Gets the permitted subtypes (concrete records).
     *
     * @return the list of subtypes
     */
    public List<ClassManifest> subtypes() {
        return subtypes;
    }

    /**
     * Gets the fields common to all subtypes.
     *
     * @return the list of common fields
     */
    public List<VariableManifest> commonFields() {
        return commonFields;
    }

    /**
     * Gets all fields from all subtypes (union of all fields).
     *
     * @return the list of all fields
     */
    public List<VariableManifest> allFields() {
        return allFields;
    }

    /**
     * Gets the package name of the sealed interface.
     *
     * @return the package name
     */
    public String packageName() {
        return type.packageName();
    }

    /**
     * Gets the simple name of the sealed interface.
     *
     * @return the simple name
     */
    public String simpleName() {
        return type.simpleName();
    }

    /**
     * Gets the qualified name of the sealed interface.
     *
     * @return the qualified name
     */
    public String qualifiedName() {
        return packageName() + '.' + simpleName();
    }

    /**
     * Gets the class name of the sealed interface.
     *
     * @return the type name
     */
    public TypeName className() {
        return type.asTypeName();
    }

    /**
     * Checks if the polymorphic aggregate has the given annotation.
     *
     * @param annotationType
     *         the annotation type
     * @param <T>
     *         the annotation type
     * @return the set of annotations of the given type
     */
    public <T extends Annotation> Set<T> annotationsOfType(Class<T> annotationType) {
        return type.annotationsOfType(annotationType);
    }

    /**
     * Checks if the aggregate has the {@link DbMigration} annotation.
     *
     * @return true if the aggregate has {@link DbMigration} annotation
     */
    public boolean hasDbMigration() {
        return !annotationsOfType(DbMigration.class).isEmpty();
    }

    @Override
    public String toString() {
        return type.toString();
    }
}
