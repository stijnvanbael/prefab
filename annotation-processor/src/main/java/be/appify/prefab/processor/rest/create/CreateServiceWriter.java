package be.appify.prefab.processor.rest.create;

import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.processor.ClassManifest;
import be.appify.prefab.processor.PolymorphicAggregateManifest;
import be.appify.prefab.processor.PrefabContext;
import be.appify.prefab.processor.VariableManifest;
import be.appify.prefab.processor.audit.AuditFields;
import be.appify.prefab.processor.rest.PathVariables;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterSpec;
import com.palantir.javapoet.TypeName;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;

import static org.apache.commons.text.WordUtils.uncapitalize;

public class CreateServiceWriter {
    MethodSpec createMethod(ClassManifest manifest, ExecutableElement constructor, PrefabContext context) {
        var tenantField = manifest.tenantIdField();
        var method = MethodSpec.methodBuilder("create")
                .addModifiers(Modifier.PUBLIC)
                .returns(String.class)
                .addStatement("log.debug($S, $T.class.getSimpleName())", "Creating new {}", manifest.className());
        addConstructorArgs(method, manifest, manifest.simpleName(), manifest.packageName(), manifest.type().asTypeName(),
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
        addConstructorArgs(method, subtype, leafName, subtype.packageName(), subtype.type().asTypeName(),
                subtype.fields(), constructor, context);
        return saveAndReturnId(polymorphic, subtype, method);
    }

    private MethodSpec saveAndReturnId(PolymorphicAggregateManifest polymorphic, ClassManifest subtype, MethodSpec.Builder method) {
        var idName = subtype.idField().map(VariableManifest::name).orElse("id");
        var idSuffix = subtype.idField().filter(f -> f.type().isSingleValueType())
                .map(f -> ".%s()".formatted(f.type().singleValueAccessor())).orElse("");
        return saveAndReturnId(method, polymorphic.simpleName(), idName, idSuffix);
    }

    MethodSpec createMethodForPolymorphicUnion(
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
        addConstructorArgsForUnion(method, polymorphic, subtype, leafName, constructor, context);
        return saveAndReturnId(polymorphic, subtype, method);
    }

    private void addConstructorArgsForUnion(
            MethodSpec.Builder method,
            PolymorphicAggregateManifest polymorphic,
            ClassManifest subtype,
            String leafName,
            ExecutableElement constructor,
            PrefabContext context
    ) {
        if (constructor.getParameters().isEmpty()) {
            method.addStatement("var aggregate = new $T()", subtype.type().asTypeName());
            return;
        }
        var parentName = parentFieldName(subtype);
        var params = constructor.getParameters().stream()
                .map(param -> VariableManifest.of(param, context.processingEnvironment()))
                .toList();
        parentName.ifPresent(name -> {
            var parentParam = params.stream().filter(p -> name.equals(p.name())).findFirst().orElseThrow();
            method.addParameter(String.class, parentParam.name() + "Id");
        });
        var hasBodyParams = params.stream()
                .anyMatch(p -> parentName.map(name -> !name.equals(p.name())).orElse(true));
        if (hasBodyParams) {
            var unionName = "Create%sRequest".formatted(polymorphic.simpleName());
            var nestedClass = ClassName.get(polymorphic.packageName() + ".application", unionName,
                    "Create%sRequest".formatted(leafName));
            method.addParameter(ParameterSpec.builder(nestedClass, "request")
                    .addAnnotation(Valid.class)
                    .build());
        }
        method.addStatement("var aggregate = new $T($L)", subtype.type().asTypeName(),
                params.stream()
                        .map(param -> resolveParam(subtype, param, parentName, Set.of(), context))
                        .collect(CodeBlock.joining(", ")));
    }

    private void addConstructorArgs(
            MethodSpec.Builder method,
            ClassManifest manifest,
            String requestRecordPrefix,
            String packageName,
            TypeName typeName,
            List<VariableManifest> fields,
            ExecutableElement constructor,
            PrefabContext context
    ) {
        if (constructor.getParameters().isEmpty()) {
            method.addStatement("var aggregate = new $T()", typeName);
            return;
        }
        var create = constructor.getAnnotation(Create.class);
        var pathVarNames = PathVariables.extractFrom(create != null ? create.path() : "");
        var parentName = parentFieldName(manifest);
        var params = constructor.getParameters().stream()
                .map(param -> VariableManifest.of(param, context.processingEnvironment()))
                .toList();
        var bodyParams = params.stream()
                .filter(p -> parentName.map(name -> !name.equals(p.name())).orElse(true))
                .filter(p -> !pathVarNames.contains(p.name()))
                .toList();
        parentName.ifPresent(name -> {
            var parentParam = params.stream().filter(p -> name.equals(p.name())).findFirst().orElseThrow();
            method.addParameter(String.class, parentParam.name() + "Id");
        });
        params.stream()
                .filter(p -> pathVarNames.contains(p.name()))
                .forEach(p -> method.addParameter(String.class, p.name()));
        if (!bodyParams.isEmpty()) {
            method.addParameter(ParameterSpec.builder(
                            ClassName.get("%s.application".formatted(packageName),
                                    "Create%sRequest".formatted(requestRecordPrefix)), "request")
                    .addAnnotation(Valid.class)
                    .build());
        }
        var constructorArgs = params.stream()
                .map(param -> resolveParam(manifest, param, parentName, pathVarNames, context))
                .collect(CodeBlock.joining(", "));
        method.addStatement("var aggregate = new $T($L)", typeName, constructorArgs);
    }

    private CodeBlock resolveParam(
            ClassManifest manifest,
            VariableManifest param,
            Optional<String> parentFieldName,
            Set<String> pathVarNames,
            PrefabContext context
    ) {
        if (parentFieldName.map(name -> name.equals(param.name())).orElse(false)) {
            if (!param.type().parameters().isEmpty()) {
                return CodeBlock.of("new $T<>($NId)",
                        ClassName.get("be.appify.prefab.core.service", "Reference"), param.name());
            }
            var repoName = uncapitalize(topLevelName(param.type().simpleName())) + "Repository";
            return CodeBlock.of("$N.findById($NId).orElseThrow()", repoName, param.name());
        }
        if (pathVarNames.contains(param.name())) {
            return CodeBlock.of("$N", param.name());
        }
        return context.requestParameterMapper().mapRequestParameter(param);
    }

    public static Optional<String> parentFieldName(ClassManifest manifest) {
        return manifest.parent()
                .filter(parent -> !parent.type().parameters().isEmpty())
                .map(VariableManifest::name);
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

    private static String topLevelName(String simpleName) {
        var dotIndex = simpleName.indexOf('.');
        return dotIndex >= 0 ? simpleName.substring(0, dotIndex) : simpleName;
    }

    private static String leafName(String simpleName) {
        var dotIndex = simpleName.lastIndexOf('.');
        return dotIndex >= 0 ? simpleName.substring(dotIndex + 1) : simpleName;
    }
}
