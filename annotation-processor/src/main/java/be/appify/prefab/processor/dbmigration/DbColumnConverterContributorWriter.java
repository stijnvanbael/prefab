package be.appify.prefab.processor.dbmigration;

import be.appify.prefab.core.annotations.DbColumn;
import be.appify.prefab.core.annotations.OutputTarget;
import be.appify.prefab.processor.ClassManifest;
import be.appify.prefab.processor.OutputTargetFileOutput;
import be.appify.prefab.processor.PrefabContext;
import be.appify.prefab.processor.FileOutput;
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
import javax.tools.Diagnostic;
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

    private final FileOutput fileWriter;
    private final PrefabContext context;

    DbColumnConverterContributorWriter(PrefabContext context) {
        this.context = context;
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
                .filter(this::hasDbColumnConverters)
                .collect(Collectors.groupingBy(ClassManifest::packageName));

        byPackage.forEach((packageName, packageManifests) -> {
            var converterTypes = packageManifests.stream()
                    .flatMap(m -> m.fields().stream())
                    .filter(field -> field.hasAnnotation(DbColumn.class))
                    .map(this::resolveConverterType)
                    .filter(type -> !isVoidOrUnresolved(type))
                    .distinct()
                    .toList();

            if (!converterTypes.isEmpty()) {
                writeContributorClass(packageName, converterTypes);
            }
        });
    }

    private boolean hasDbColumnConverters(ClassManifest manifest) {
        return manifest.fields().stream()
                .filter(field -> field.hasAnnotation(DbColumn.class))
                .anyMatch(field -> !isVoidOrUnresolved(resolveConverterType(field)));
    }

    /**
     * Resolves the converter {@link TypeMirror} for a {@code @DbColumn}-annotated field, either from
     * {@code converter()} or, when that is void, from the fully qualified {@code converterName()}.
     * Uses the {@link MirroredTypeException} pattern because class-valued annotation attributes
     * are not directly accessible via the Java reflection proxy during annotation processing.
     */
    private TypeMirror resolveConverterType(VariableManifest field) {
        var annotation = field.getAnnotation(DbColumn.class)
                .orElseThrow()
                .value();
        TypeMirror classType;
        try {
            annotation.converter(); // always throws MirroredTypeException for class attributes
            throw new AssertionError("Expected MirroredTypeException when reading @DbColumn.converter()");
        } catch (MirroredTypeException e) {
            classType = e.getTypeMirror();
        }
        if (!isVoid(classType) || annotation.converterName().isBlank()) {
            return classType;
        }
        return resolveByName(annotation.converterName(), field);
    }

    private TypeMirror resolveByName(String name, VariableManifest field) {
        var env = context.processingEnvironment();
        var element = env.getElementUtils().getTypeElement(name);
        if (element == null) {
            env.getMessager().printMessage(Diagnostic.Kind.ERROR,
                    "@DbColumn.converterName() '%s' on field '%s' cannot be resolved to a class"
                            .formatted(name, field.name()), field.element());
            return env.getTypeUtils().getNoType(TypeKind.VOID);
        }
        var converterType = env.getElementUtils().getTypeElement("org.springframework.core.convert.converter.Converter");
        if (converterType != null && !env.getTypeUtils().isAssignable(
                env.getTypeUtils().erasure(element.asType()), env.getTypeUtils().erasure(converterType.asType()))) {
            env.getMessager().printMessage(Diagnostic.Kind.ERROR,
                    "@DbColumn.converterName() '%s' on field '%s' must implement Spring's Converter"
                            .formatted(name, field.name()), field.element());
            return env.getTypeUtils().getNoType(TypeKind.VOID);
        }
        return element.asType();
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

