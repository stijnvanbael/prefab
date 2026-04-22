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
            List<ClassManifest> subtypes
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
        return new PolymorphicAggregateManifest(typeManifest, subtypes);
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

    public TypeManifest type() {
        return type;
    }

    public List<ClassManifest> subtypes() {
        return subtypes;
    }

    public List<VariableManifest> commonFields() {
        return commonFields;
    }

    /** Returns the union of all fields across all subtypes. */
    public List<VariableManifest> allFields() {
        return allFields;
    }

    public String packageName() {
        return type.packageName();
    }

    public String simpleName() {
        return type.simpleName();
    }

    public TypeName className() {
        return type.asTypeName();
    }

    public <T extends Annotation> Set<T> annotationsOfType(Class<T> annotationType) {
        return type.annotationsOfType(annotationType);
    }

    public boolean isDbMigrationEnabled() {
        var annotations = annotationsOfType(DbMigration.class);
        return annotations.stream().allMatch(DbMigration::enabled);
    }

    @Override
    public String toString() {
        return type.toString();
    }
}
