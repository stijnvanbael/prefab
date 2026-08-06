package be.appify.prefab.processor;

import com.palantir.javapoet.AnnotationSpec;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.FieldSpec;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterSpec;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeSpec;
import com.palantir.javapoet.TypeVariableName;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.WildcardTypeName;

import java.util.ArrayList;
import javax.lang.model.element.Modifier;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
        enrichWithBuilder(recordBuilder, recordType, fields, Map.of());
    }

    /**
     * Adds a nested {@code Builder} class and a static {@code builder()} factory method to the given record builder,
     * pre-initialising builder fields with the supplied default literals.
     *
     * @param recordBuilder the record {@link TypeSpec.Builder} to enrich
     * @param recordType    the {@link ClassName} of the record being built
     * @param fields        the record components (name and type used; annotations are ignored)
     * @param fieldDefaults map of field name to JavaPoet initialiser literal (e.g. {@code "\"hello\""}, {@code "42"})
     */
    public void enrichWithBuilder(TypeSpec.Builder recordBuilder, ClassName recordType,
            List<ParameterSpec> fields, Map<String, String> fieldDefaults) {
        recordBuilder.addMethod(builderFactoryMethod(recordType));
        recordBuilder.addType(nestedBuilderClass(recordType, fields, fieldDefaults));
    }

    /**
     * Builds a standalone builder type that can be emitted as a top-level class.
     *
     * @param builderType   the class name of the standalone builder
     * @param targetType    the target type constructed by {@code build()}
     * @param fields        builder fields and setter/add method source
     * @return the generated standalone builder class
     */
    public TypeSpec standaloneBuilderClass(ClassName builderType, TypeName targetType, List<ParameterSpec> fields) {
        return standaloneBuilderClass(builderType, targetType, fields, Map.of());
    }

    /**
     * Builds a standalone builder type that can be emitted as a top-level class.
     *
     * @param builderType   the class name of the standalone builder
     * @param targetType    the target type constructed by {@code build()}
     * @param fields        builder fields and setter/add method source
     * @param fieldDefaults map of field name to JavaPoet initialiser literal
     * @return the generated standalone builder class
     */
    public TypeSpec standaloneBuilderClass(ClassName builderType, TypeName targetType,
            List<ParameterSpec> fields, Map<String, String> fieldDefaults) {
        var builder = TypeSpec.classBuilder(builderType.simpleName())
                .addModifiers(Modifier.PUBLIC);
        return enrichBuilderClass(builder, builderType, targetType, fields, fieldDefaults).build();
    }

    private MethodSpec builderFactoryMethod(ClassName recordType) {
        var builderType = ClassName.get(recordType.packageName(), recordType.simpleName() + ".Builder");
        return MethodSpec.methodBuilder("builder")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(ParameterizedTypeName.get(builderType, WildcardTypeName.subtypeOf(Object.class)))
                .addStatement("return new $T<>()", builderType)
                .build();
    }

    private TypeSpec nestedBuilderClass(ClassName recordType, List<ParameterSpec> fields,
            Map<String, String> fieldDefaults) {
        var builder = TypeSpec.classBuilder(BUILDER)
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC);
        return enrichBuilderClass(builder, ClassName.get("", BUILDER), recordType, fields, fieldDefaults).build();
    }

    private TypeSpec.Builder enrichBuilderClass(TypeSpec.Builder builder,
            ClassName selfRawType, TypeName targetType,
            List<ParameterSpec> fields, Map<String, String> fieldDefaults) {
        var selfTypeName = TypeVariableName.get("SELF");
        var selfBound = ParameterizedTypeName.get(selfRawType, selfTypeName);
        var selfTypeVar = TypeVariableName.get("SELF", selfBound);
        builder.addTypeVariable(selfTypeVar);

        fields.forEach(field -> {
            builder.addField(buildField(field, fieldDefaults));
            builder.addMethod(withMethod(field));
            if (isListField(field)) {
                builder.addMethod(addMethod(field));
            }
        });

        builder.addMethod(selfMethod());
        builder.addMethod(buildMethod(targetType, fields));
        return builder;
    }

    private MethodSpec selfMethod() {
        return MethodSpec.methodBuilder("self")
                .addModifiers(Modifier.PROTECTED)
                .addAnnotation(AnnotationSpec.builder(SuppressWarnings.class)
                        .addMember("value", "$S", "unchecked")
                        .build())
                .returns(TypeVariableName.get("SELF"))
                .addStatement("return (SELF) this")
                .build();
    }

    private FieldSpec buildField(ParameterSpec field, Map<String, String> fieldDefaults) {
        var defaultLiteral = fieldDefaults.get(field.name());
        if (defaultLiteral != null) {
            return FieldSpec.builder(field.type(), field.name(), Modifier.PRIVATE)
                    .initializer(defaultLiteral)
                    .build();
        }
        return FieldSpec.builder(field.type(), field.name(), Modifier.PRIVATE).build();
    }

    private MethodSpec withMethod(ParameterSpec field) {
        var plainParam = ParameterSpec.builder(field.type(), field.name()).build();
        return MethodSpec.methodBuilder(setterMethodName(field.name()))
                .addModifiers(Modifier.PUBLIC)
                .returns(TypeVariableName.get("SELF"))
                .addParameter(plainParam)
                .addStatement("this.$1N = $1N", field.name())
                .addStatement("return self()")
                .build();
    }

    private MethodSpec addMethod(ParameterSpec field) {
        var itemParam = ParameterSpec.builder(listItemType(field), "item").build();
        return MethodSpec.methodBuilder(addMethodName(field.name()))
                .addModifiers(Modifier.PUBLIC)
                .returns(TypeVariableName.get("SELF"))
                .addParameter(itemParam)
                .beginControlFlow("if (this.$N == null)", field.name())
                .addStatement("this.$N = new $T<>()", field.name(), ArrayList.class)
                .nextControlFlow("else if (!(this.$N instanceof $T))", field.name(), ArrayList.class)
                .addStatement("this.$N = new $T<>(this.$N)", field.name(), ArrayList.class, field.name())
                .endControlFlow()
                .addStatement("this.$N.add(item)", field.name())
                .addStatement("return self()")
                .build();
    }

    private boolean isListField(ParameterSpec field) {
        return field.type() instanceof ParameterizedTypeName parameterizedType
                && parameterizedType.rawType().equals(ClassName.get(List.class));
    }

    private TypeName listItemType(ParameterSpec field) {
        if (field.type() instanceof ParameterizedTypeName parameterizedType && !parameterizedType.typeArguments().isEmpty()) {
            return parameterizedType.typeArguments().getFirst();
        }
        return TypeName.get(Object.class);
    }

    private String setterMethodName(String fieldName) {
        return setterPrefix.isEmpty() ? fieldName : setterPrefix + capitalize(fieldName);
    }

    private String addMethodName(String fieldName) {
        return "add" + capitalize(fieldName);
    }

    private MethodSpec buildMethod(TypeName targetType, List<ParameterSpec> fields) {
        var args = fields.stream().map(ParameterSpec::name).collect(Collectors.joining(", "));
        return MethodSpec.methodBuilder("build")
                .addModifiers(Modifier.PUBLIC)
                .returns(targetType)
                .addStatement("return new $T($L)", targetType, args)
                .build();
    }
}
