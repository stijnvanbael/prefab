package be.appify.prefab.processor;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterSpec;
import com.palantir.javapoet.TypeSpec;
import java.util.List;
import java.util.stream.Collectors;
import javax.lang.model.element.Modifier;

import static org.apache.commons.text.WordUtils.capitalize;

/**
 * Generates a nested {@code Builder} class and a static {@code builder()} factory method
 * for a record {@link TypeSpec.Builder}.
 *
 * <p>The setter method prefix (e.g. {@code with}) is configurable. Pass the desired prefix via
 * the {@code prefab.builder.setterPrefix} annotation-processor option (compiler {@code -A} flag).
 * An empty prefix produces method names equal to the field name (e.g. {@code name(String name)}).
 */
public class BuilderWriter {

    private static final String BUILDER = "Builder";

    private final String setterPrefix;

    /**
     * Creates a {@code BuilderWriter} with a custom setter prefix.
     *
     * @param setterPrefix the prefix prepended to the capitalised field name; use {@code ""}
     *                     for prefix-less methods where the method name equals the field name
     */
    public BuilderWriter(String setterPrefix) {
        this.setterPrefix = setterPrefix;
    }

    /**
     * Adds a nested {@code Builder} class and a static {@code builder()} factory method to the given record builder.
     *
     * @param recordBuilder the record {@link TypeSpec.Builder} to enrich
     * @param recordType    the {@link ClassName} of the record being built
     * @param fields        the record components (name and type used; annotations are ignored)
     */
    public void enrichWithBuilder(TypeSpec.Builder recordBuilder, ClassName recordType, List<ParameterSpec> fields) {
        recordBuilder.addMethod(builderFactoryMethod());
        recordBuilder.addType(buildNestedBuilderClass(recordType, fields));
    }

    private MethodSpec builderFactoryMethod() {
        return MethodSpec.methodBuilder("builder")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(ClassName.get("", BUILDER))
                .addStatement("return new $L()", BUILDER)
                .build();
    }

    private TypeSpec buildNestedBuilderClass(ClassName recordType, List<ParameterSpec> fields) {
        var builder = TypeSpec.classBuilder(BUILDER)
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL);

        fields.forEach(field -> {
            builder.addField(field.type(), field.name(), Modifier.PRIVATE);
            builder.addMethod(withMethod(field));
        });

        builder.addMethod(buildMethod(recordType, fields));

        return builder.build();
    }

    private MethodSpec withMethod(ParameterSpec field) {
        var plainParam = ParameterSpec.builder(field.type(), field.name()).build();
        return MethodSpec.methodBuilder(setterMethodName(field.name()))
                .addModifiers(Modifier.PUBLIC)
                .returns(ClassName.get("", BUILDER))
                .addParameter(plainParam)
                .addStatement("this.$1N = $1N", field.name())
                .addStatement("return this")
                .build();
    }

    private String setterMethodName(String fieldName) {
        return setterPrefix.isEmpty() ? fieldName : setterPrefix + capitalize(fieldName);
    }

    private MethodSpec buildMethod(ClassName recordType, List<ParameterSpec> fields) {
        var args = fields.stream().map(ParameterSpec::name).collect(Collectors.joining(", "));
        return MethodSpec.methodBuilder("build")
                .addModifiers(Modifier.PUBLIC)
                .returns(recordType)
                .addStatement("return new $T($L)", recordType, args)
                .build();
    }
}

