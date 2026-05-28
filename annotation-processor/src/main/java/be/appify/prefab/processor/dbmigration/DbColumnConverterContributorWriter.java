package be.appify.prefab.processor.dbmigration;

import be.appify.prefab.core.annotations.DbColumn;
import be.appify.prefab.core.annotations.OutputTarget;
import be.appify.prefab.processor.ClassManifest;
import be.appify.prefab.processor.OutputTargetFileOutput;
import be.appify.prefab.processor.PrefabContext;
import be.appify.prefab.processor.TestFileOutput;
import be.appify.prefab.processor.VariableManifest;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;
import java.util.List;
import java.util.stream.Collectors;
import javax.lang.model.element.Modifier;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.springframework.stereotype.Component;

/**
 * Generates a {@code @Component} class that implements {@code DbColumnConverterContributor},
 * registering all converters declared via {@code @DbColumn(converter = ...)} with
 * {@code JdbcCustomConversions} at application startup.
 *
 * <p>One contributor class is generated per aggregate package when at least one aggregate field
 * carries a {@code @DbColumn(converter = ...)} annotation with a non-void converter class.</p>
 */
class DbColumnConverterContributorWriter {

    private static final String CONTRIBUTOR_INTERFACE =
            "be.appify.prefab.core.spring.data.jdbc.DbColumnConverterContributor";

    private final TestFileOutput fileWriter;

    DbColumnConverterContributorWriter(PrefabContext context) {
        this.fileWriter = new OutputTargetFileOutput(context, "infrastructure.persistence", OutputTarget.MAIN);
    }

    /**
     * Generates contributor classes for all aggregates that declare {@code @DbColumn(converter = ...)} fields.
     * Aggregates are grouped by package; one contributor class is generated per package.
     *
     * @param manifests all resolved aggregate manifests
     */
    void writeContributors(List<ClassManifest> manifests) {
        var byPackage = manifests.stream()
                .filter(DbColumnConverterContributorWriter::hasDbColumnConverters)
                .collect(Collectors.groupingBy(ClassManifest::packageName));

        byPackage.forEach((packageName, packageManifests) -> {
            var converterTypes = packageManifests.stream()
                    .flatMap(m -> m.fields().stream())
                    .filter(field -> field.hasAnnotation(DbColumn.class))
                    .map(DbColumnConverterContributorWriter::resolveConverterType)
                    .filter(type -> !isVoidOrUnresolved(type))
                    .distinct()
                    .toList();

            if (!converterTypes.isEmpty()) {
                writeContributorClass(packageName, converterTypes);
            }
        });
    }

    private static boolean hasDbColumnConverters(ClassManifest manifest) {
        return manifest.fields().stream()
                .filter(field -> field.hasAnnotation(DbColumn.class))
                .anyMatch(field -> !isVoidOrUnresolved(resolveConverterType(field)));
    }

    /**
     * Resolves the converter {@link TypeMirror} for a {@code @DbColumn}-annotated field.
     * Uses the {@link MirroredTypeException} pattern because class-valued annotation attributes
     * are not directly accessible via the Java reflection proxy during annotation processing.
     */
    private static TypeMirror resolveConverterType(VariableManifest field) {
        var annotation = field.getAnnotation(DbColumn.class)
                .orElseThrow()
                .value();
        try {
            annotation.converter(); // always throws MirroredTypeException for class attributes
            throw new AssertionError("Expected MirroredTypeException when reading @DbColumn.converter()");
        } catch (MirroredTypeException e) {
            return e.getTypeMirror();
        }
    }

    private static boolean isVoid(TypeMirror type) {
        return type.toString().equals("void");
    }

    private static boolean isVoidOrUnresolved(TypeMirror type) {
        return isVoid(type) || type.getKind() == TypeKind.ERROR;
    }

    private void writeContributorClass(String packageName, List<TypeMirror> converterTypes) {
        var className = "DbColumnConverterContributor";
        var contributorInterface = ClassName.bestGuess(CONTRIBUTOR_INTERFACE);
        var listOfObject = ParameterizedTypeName.get(List.class, Object.class);

        var convertersCode = converterTypes.stream()
                .map(type -> CodeBlock.of("new $T()", TypeName.get(type)))
                .collect(CodeBlock.joining(",\n"));

        var convertersMethod = MethodSpec.methodBuilder("converters")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(listOfObject)
                .addStatement("return $T.of($L)", List.class, convertersCode)
                .build();

        var typeSpec = TypeSpec.classBuilder(className)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Component.class)
                .addSuperinterface(contributorInterface)
                .addMethod(convertersMethod)
                .build();

        fileWriter.writeFile(packageName, className, typeSpec);
    }
}

