package be.appify.prefab.processor.rest.create;

import be.appify.prefab.processor.ClassManifest;
import be.appify.prefab.processor.PolymorphicAggregateManifest;
import be.appify.prefab.processor.PrefabContext;
import be.appify.prefab.processor.VariableManifest;
import be.appify.prefab.processor.audit.AuditFields;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterSpec;
import com.palantir.javapoet.TypeName;
import jakarta.validation.Valid;
import java.util.List;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;

import static org.apache.commons.text.WordUtils.uncapitalize;

class CreateServiceWriter {
    MethodSpec createMethod(ClassManifest manifest, ExecutableElement constructor, PrefabContext context) {
        var tenantField = manifest.tenantIdField();
        var method = MethodSpec.methodBuilder("create")
                .addModifiers(Modifier.PUBLIC)
                .returns(String.class)
                .addStatement("log.debug($S, $T.class.getSimpleName())", "Creating new {}", manifest.className());
        addConstructorArgs(method, manifest.simpleName(), manifest.packageName(), manifest.type().asTypeName(),
                manifest.fields(), constructor, context);
        tenantField.ifPresent(tf -> method.addStatement("aggregate = new $T($L)",
                manifest.type().asTypeName(), reconstructionArgs(manifest, tf)));
        addAuditForCreate(method, manifest);
        return saveAndReturnId(method, manifest.simpleName(), manifest.idField().map(VariableManifest::name).orElse("id"),
                manifest.idField().filter(f -> f.type().isSingleValueType())
                        .map(f -> ".%s()".formatted(f.type().singleValueAccessor())).orElse(""));
    }

    MethodSpec createMethodForPolymorphic(
            PolymorphicAggregateManifest polymorphic,
            ClassManifest subtype,
            ExecutableElement constructor,
            PrefabContext context
    ) {
        var leafName = leafName(subtype.simpleName());
        var methodName = "create" + leafName;
        var method = MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.PUBLIC)
                .returns(String.class)
                .addStatement("log.debug($S, $T.class.getSimpleName())", "Creating new {}",
                        subtype.type().asTypeName());
        addConstructorArgs(method, leafName, subtype.packageName(), subtype.type().asTypeName(),
                subtype.fields(), constructor, context);
        var idName = subtype.idField().map(VariableManifest::name).orElse("id");
        var idSuffix = subtype.idField().filter(f -> f.type().isSingleValueType())
                .map(f -> ".%s()".formatted(f.type().singleValueAccessor())).orElse("");
        return saveAndReturnId(method, polymorphic.simpleName(), idName, idSuffix);
    }

    private void addConstructorArgs(
            MethodSpec.Builder method,
            String requestRecordPrefix,
            String packageName,
            TypeName typeName,
            List<VariableManifest> fields,
            ExecutableElement constructor,
            PrefabContext context
    ) {
        if (constructor.getParameters().isEmpty()) {
            method.addStatement("var aggregate = new $T()", typeName);
        } else {
            method.addParameter(ParameterSpec.builder(
                            ClassName.get("%s.application".formatted(packageName),
                                    "Create%sRequest".formatted(requestRecordPrefix)), "request")
                            .addAnnotation(Valid.class)
                            .build())
                    .addStatement("var aggregate = new $T($L)", typeName,
                            constructor.getParameters().stream()
                                    .map(param -> VariableManifest.of(param, context.processingEnvironment()))
                                    .map(context.requestParameterMapper()::mapRequestParameter)
                                    .collect(CodeBlock.joining(", ")));
        }
    }

    private static MethodSpec saveAndReturnId(MethodSpec.Builder method, String aggregateName, String idName, String idSuffix) {
        return method
                .addStatement("%sRepository.save(aggregate)".formatted(uncapitalize(aggregateName)))
                .addStatement("return aggregate.$N()$L", idName, idSuffix)
                .build();
    }

    private static void addAuditForCreate(MethodSpec.Builder method, ClassManifest manifest) {
        if (AuditFields.hasAuditFields(manifest)) {
            method.addStatement("aggregate = new $T($L)", manifest.type().asTypeName(),
                    AuditFields.createReconstructionArgs(manifest.fields()));
        }
    }

    private static CodeBlock reconstructionArgs(ClassManifest manifest, VariableManifest tenantField) {
        return manifest.fields().stream()
                .map(field -> field.name().equals(tenantField.name())
                        ? CodeBlock.of("tenantContextProvider.currentTenantId()")
                        : CodeBlock.of("aggregate.$N()", field.name()))
                .collect(CodeBlock.joining(", "));
    }

    private static String leafName(String simpleName) {
        var dotIndex = simpleName.lastIndexOf('.');
        return dotIndex >= 0 ? simpleName.substring(dotIndex + 1) : simpleName;
    }
}
