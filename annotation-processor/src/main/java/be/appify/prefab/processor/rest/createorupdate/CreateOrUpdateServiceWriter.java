package be.appify.prefab.processor.rest.createorupdate;

import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.processor.ClassManifest;
import be.appify.prefab.processor.PrefabContext;
import be.appify.prefab.processor.VariableManifest;
import be.appify.prefab.processor.audit.AuditFields;
import be.appify.prefab.processor.rest.PathVariables;
import be.appify.prefab.processor.rest.create.CreateServiceWriter;
import be.appify.prefab.processor.rest.update.UpdateManifest;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterSpec;
import jakarta.validation.Valid;
import java.util.Set;
import java.util.stream.Collectors;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;

import static org.apache.commons.text.WordUtils.uncapitalize;

class CreateOrUpdateServiceWriter {

    MethodSpec createOrUpdateMethod(
            ClassManifest manifest,
            CreateOrUpdateManifest createOrUpdate,
            PrefabContext context
    ) {
        var create = createOrUpdate.createConstructor().getAnnotation(Create.class);
        var pathVarNames = PathVariables.extractFrom(create.path());
        var lookupVar = createOrUpdate.lookupVariable();
        var update = createOrUpdate.updateManifest();
        var repositoryName = uncapitalize(manifest.simpleName()) + "Repository";

        var params = createOrUpdate.createConstructor().getParameters().stream()
                .map(p -> VariableManifest.of(p, context.processingEnvironment()))
                .toList();
        var parentName = CreateServiceWriter.parentFieldName(manifest);
        var bodyParams = params.stream()
                .filter(p -> !pathVarNames.contains(p.name()))
                .filter(p -> parentName.map(name -> !name.equals(p.name())).orElse(true))
                .toList();

        var method = MethodSpec.methodBuilder("create")
                .addModifiers(Modifier.PUBLIC)
                .returns(String.class)
                .addParameter(String.class, lookupVar);

        update.pathParameters().stream()
                .filter(p -> !p.name().equals(lookupVar))
                .forEach(p -> method.addParameter(String.class, p.name()));

        if (!bodyParams.isEmpty()) {
            var requestType = ClassName.get(
                    "%s.application".formatted(manifest.packageName()),
                    "Create%sRequest".formatted(manifest.simpleName()));
            method.addParameter(ParameterSpec.builder(requestType, "request")
                    .addAnnotation(Valid.class)
                    .build());
        }

        method.addStatement("log.debug($S, $T.class.getSimpleName(), $N)",
                "Creating or updating {} with id: {}", manifest.className(), lookupVar);

        var createArgs = buildCreateArgs(params, pathVarNames, parentName, context);
        var updateDomainCall = buildUpdateDomainCall(update, bodyParams, pathVarNames);
        var idFieldOpt = manifest.idField();
        var idName = idFieldOpt.map(VariableManifest::name).orElse("id");
        var idSuffix = idFieldOpt.filter(f -> f.type().isSingleValueType())
                .map(f -> ".%s()".formatted(f.type().singleValueAccessor()))
                .orElse("");

        addCreateOrUpdateStatement(method, manifest, repositoryName, lookupVar, update,
                createArgs, updateDomainCall, idName, idSuffix, bodyParams.isEmpty());

        return method.build();
    }

    private void addCreateOrUpdateStatement(
            MethodSpec.Builder method,
            ClassManifest manifest,
            String repositoryName,
            String lookupVar,
            UpdateManifest update,
            CodeBlock createArgs,
            CodeBlock updateDomainCall,
            String idName,
            String idSuffix,
            boolean noBodyParams
    ) {
        var hasAudit = AuditFields.hasAuditFields(manifest);

        if (hasAudit) {
            addCreateOrUpdateWithAudit(method, manifest, repositoryName, lookupVar, update,
                    createArgs, updateDomainCall, idName, idSuffix, noBodyParams);
        } else {
            addCreateOrUpdateBasic(method, repositoryName, lookupVar, createArgs,
                    updateDomainCall, idName, idSuffix, noBodyParams, manifest);
        }
    }

    private void addCreateOrUpdateBasic(
            MethodSpec.Builder method,
            String repositoryName,
            String lookupVar,
            CodeBlock createArgs,
            CodeBlock updateDomainCall,
            String idName,
            String idSuffix,
            boolean noBodyParams,
            ClassManifest manifest
    ) {
        var saveAndReturn = updateReturnBlock(manifest, repositoryName, idName, idSuffix);
        method.addStatement("""
                        return $N.findById($N)
                                .map(aggregate -> {
                                    $L
                                    $L
                                })
                                .orElseGet(() -> {
                                    var aggregate = new $T($L);
                                    $L
                                })""",
                repositoryName, lookupVar,
                updateDomainCall,
                saveAndReturn,
                manifest.type().asTypeName(), createArgs,
                saveAndReturn);
    }

    private void addCreateOrUpdateWithAudit(
            MethodSpec.Builder method,
            ClassManifest manifest,
            String repositoryName,
            String lookupVar,
            UpdateManifest update,
            CodeBlock createArgs,
            CodeBlock updateDomainCall,
            String idName,
            String idSuffix,
            boolean noBodyParams
    ) {
        var saveAndReturn = updateReturnBlock(manifest, repositoryName, idName, idSuffix);
        var createSaveAndReturn = createReturnBlock(manifest, repositoryName, idName, idSuffix);
        method.addStatement("""
                        return $N.findById($N)
                                .map(aggregate -> {
                                    $L
                                    aggregate = new $T($L);
                                    $L
                                })
                                .orElseGet(() -> {
                                    var aggregate = new $T($L);
                                    aggregate = new $T($L);
                                    $L
                                })""",
                repositoryName, lookupVar,
                updateDomainCall,
                manifest.type().asTypeName(),
                AuditFields.updateReconstructionArgs(manifest.fields()),
                saveAndReturn,
                manifest.type().asTypeName(), createArgs,
                manifest.type().asTypeName(),
                AuditFields.createReconstructionArgs(manifest.fields()),
                createSaveAndReturn);
    }

    private static CodeBlock updateReturnBlock(
            ClassManifest manifest,
            String repositoryName,
            String idName,
            String idSuffix
    ) {
        return CodeBlock.of("var saved = $N.save(aggregate);\nreturn saved.$N()$L;\n",
                repositoryName, idName, idSuffix);
    }

    private static CodeBlock createReturnBlock(
            ClassManifest manifest,
            String repositoryName,
            String idName,
            String idSuffix
    ) {
        return CodeBlock.of("var saved = $N.save(aggregate);\nreturn saved.$N()$L;\n",
                repositoryName, idName, idSuffix);
    }

    private CodeBlock buildCreateArgs(
            java.util.List<VariableManifest> params,
            Set<String> pathVarNames,
            java.util.Optional<String> parentName,
            PrefabContext context
    ) {
        return params.stream()
                .map(param -> {
                    if (pathVarNames.contains(param.name())) {
                        return CodeBlock.of("$N", param.name());
                    }
                    if (parentName.map(name -> name.equals(param.name())).orElse(false)) {
                        return CodeBlock.of("$N", param.name() + "Id");
                    }
                    return context.requestParameterMapper().mapRequestParameter(param);
                })
                .collect(CodeBlock.joining(", "));
    }

    private CodeBlock buildUpdateDomainCall(
            UpdateManifest update,
            java.util.List<VariableManifest> bodyParams,
            Set<String> pathVarNames
    ) {
        var bodyParamNames = bodyParams.stream()
                .map(VariableManifest::name)
                .collect(Collectors.toSet());
        var args = update.parameters().stream()
                .map(p -> {
                    if (pathVarNames.contains(p.name()) || p.name().equals("id")) {
                        return CodeBlock.of("$N", p.name());
                    }
                    if (bodyParamNames.contains(p.name())) {
                        return fromRequest(p);
                    }
                    return CodeBlock.of("aggregate.$N()", p.name());
                })
                .collect(CodeBlock.joining(", "));

        if (update.stateful()) {
            return CodeBlock.of("aggregate.$N($L);\n", update.operationName(), args);
        }
        return CodeBlock.of("aggregate = aggregate.$N($L);\n", update.operationName(), args);
    }

    private CodeBlock fromRequest(VariableManifest parameter) {
        if (parameter.type().isSingleValueType()) {
            return CodeBlock.of("request.$N() != null ? new $T(request.$N()) : null",
                    parameter.name(), parameter.type().asTypeName(), parameter.name());
        }
        return CodeBlock.of("request.$N()", parameter.name());
    }
}
