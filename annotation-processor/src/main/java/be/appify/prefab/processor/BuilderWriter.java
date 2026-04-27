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
 */
public class BuilderWriter {

    private static final String BUILDER = "Builder";

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
        return MethodSpec.methodBuilder("with" + capitalize(field.name()))
                .addModifiers(Modifier.PUBLIC)
                .returns(ClassName.get("", BUILDER))
                .addParameter(plainParam)
                .addStatement("this.$1N = $1N", field.name())
                .addStatement("return this")
                .build();
    }

    private MethodSpec buildMethod(ClassName recordType, List<ParameterSpec> fields) {
        var args = fields.stream().map(f -> f.name()).collect(Collectors.joining(", "));
        return MethodSpec.methodBuilder("build")
                .addModifiers(Modifier.PUBLIC)
                .returns(recordType)
                .addStatement("return new $T($L)", recordType, args)
                .build();
    }
}

