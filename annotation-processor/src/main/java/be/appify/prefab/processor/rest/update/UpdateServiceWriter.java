package be.appify.prefab.processor.rest.update;

import be.appify.prefab.processor.ClassManifest;
import be.appify.prefab.processor.PolymorphicAggregateManifest;
import be.appify.prefab.processor.VariableManifest;
import be.appify.prefab.processor.audit.AuditFields;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterSpec;
import com.palantir.javapoet.ParameterizedTypeName;
import jakarta.validation.Valid;
import java.util.Optional;
import javax.lang.model.element.Modifier;

import static org.apache.commons.text.WordUtils.capitalize;
import static org.apache.commons.text.WordUtils.uncapitalize;

class UpdateServiceWriter {
    MethodSpec updateMethod(ClassManifest manifest, UpdateManifest update) {
        var method = MethodSpec.methodBuilder(update.operationName())
                .addModifiers(Modifier.PUBLIC)
                .returns(ParameterizedTypeName.get(ClassName.get(Optional.class), manifest.type().asTypeName()))
                .addParameter(String.class, "id");
        if (!update.requestParameters().isEmpty()) {
            method.addParameter(ParameterSpec.builder(
                            ClassName.get("%s.application".formatted(manifest.packageName()), "%s%sRequest".formatted(
                                    manifest.simpleName(), capitalize(update.operationName()))), "request")
                    .addAnnotation(Valid.class)
                    .build());
        }
        method.addStatement("log.debug($S, $T.class.getSimpleName(), id)", "Updating {} with id: {}",
                manifest.className());
        var domainCallBlock = buildDomainCallBlock(update);
        var aggregateFunction = buildAggregateFunction(update, domainCallBlock);
        var repositoryName = uncapitalize(manifest.simpleName()) + "Repository";
        var tenantField = manifest.tenantIdField();
        var hasAudit = AuditFields.hasAuditFields(manifest);
        if (tenantField.isPresent() && hasAudit) {
            updateWithTenantAndAudit(manifest, method, repositoryName, tenantField.get(), aggregateFunction);
        } else if (tenantField.isPresent()) {
            updateWithTenant(method, repositoryName, tenantField.get(), aggregateFunction);
        } else if (hasAudit) {
            updateWithAudit(manifest, method, repositoryName, aggregateFunction);
        } else {
            updateBasic(method, repositoryName, aggregateFunction);
        }
        return method.build();
    }

    private CodeBlock buildAggregateFunction(UpdateManifest update, CodeBlock domainCallBlock) {
        var body = CodeBlock.builder();
        update.aggregateParameters().forEach(param -> {
            var repositoryName = uncapitalize(param.type().simpleName()) + "Repository";
            body.add("var $N = $N.findById(request.$NId()).orElseThrow();\n",
                    param.name(), repositoryName, param.name());
        });
        body.add(domainCallBlock);
        return body.build();
    }

    private CodeBlock buildDomainCallBlock(UpdateManifest update) {
        var args = update.parameters().stream()
                .map(this::resolveParam)
                .collect(CodeBlock.joining(", "));
        if (update.stateful()) {
            return CodeBlock.of("aggregate.$N($L);\n", update.operationName(), args);
        }
        return CodeBlock.of("aggregate = aggregate.$N($L);\n", update.operationName(), args);
    }


    private static void updateWithTenantAndAudit(
            ClassManifest manifest,
            MethodSpec.Builder method,
            String repositoryName,
            VariableManifest tenantField,
            CodeBlock aggregateFunction
    ) {
        method.addStatement("""
                        return $N.findById(id)
                                .filter(aggregate -> tenantContextProvider.currentTenantId() == null
                                        || aggregate.$N().equals(tenantContextProvider.currentTenantId()))
                                .map(aggregate -> {
                                    $L
                                    aggregate = new $T($L);
                                    return $N.save(aggregate);
                                })""",
                repositoryName,
                tenantField.name(),
                aggregateFunction,
                manifest.type().asTypeName(),
                AuditFields.updateReconstructionArgs(manifest.fields()),
                repositoryName);
    }

    private static void updateWithTenant(
            MethodSpec.Builder method,
            String repositoryName,
            VariableManifest tenantField,
            CodeBlock aggregateFunction
    ) {
        method.addStatement("""
                        return $N.findById(id)
                                .filter(aggregate -> tenantContextProvider.currentTenantId() == null
                                        || aggregate.$N().equals(tenantContextProvider.currentTenantId()))
                                .map(aggregate -> {
                                    $L
                                    return $N.save(aggregate);
                                })""",
                repositoryName,
                tenantField.name(),
                aggregateFunction,
                repositoryName);
    }

    private static void updateWithAudit(
            ClassManifest manifest,
            MethodSpec.Builder method,
            String repositoryName,
            CodeBlock aggregateFunction
    ) {
        method.addStatement("""
                        return $N.findById(id).map(aggregate -> {
                            $L
                            aggregate = new $T($L);
                            return $N.save(aggregate);
                        })""",
                repositoryName,
                aggregateFunction,
                manifest.type().asTypeName(),
                AuditFields.updateReconstructionArgs(manifest.fields()),
                repositoryName);
    }

    private static void updateBasic(
            MethodSpec.Builder method,
            String repositoryName,
            CodeBlock aggregateFunction
    ) {
        method.addStatement("""
                        return $N.findById(id).map(aggregate -> {
                            $L
                            return $N.save(aggregate);
                        })""",
                repositoryName,
                aggregateFunction,
                repositoryName);
    }

    private CodeBlock resolveParam(VariableManifest parameter) {
        if (!parameter.type().annotationsOfType(
                be.appify.prefab.core.annotations.Aggregate.class).isEmpty()) {
            return CodeBlock.of("$N", parameter.name());
        }
        return fromRequest(parameter);
    }

    private CodeBlock fromRequest(VariableManifest parameter) {
        if (parameter.type().isSingleValueType()) {
            return CodeBlock.of("request.$N() != null ? new $T(request.$N()) : null",
                    parameter.name(), parameter.type().asTypeName(), parameter.name());
        }
        return CodeBlock.of("request.$N()", parameter.name());
    }

    MethodSpec updateMethodForPolymorphic(
            PolymorphicAggregateManifest polymorphic,
            ClassManifest subtype,
            UpdateManifest update
    ) {
        var leafName = leafName(subtype.simpleName());
        var operationName = uncapitalize(leafName + capitalize(update.operationName()));
        var repositoryName = uncapitalize(polymorphic.simpleName()) + "Repository";
        var method = MethodSpec.methodBuilder(operationName)
                .addModifiers(Modifier.PUBLIC)
                .returns(ParameterizedTypeName.get(ClassName.get(Optional.class), polymorphic.className()));
        method.addParameter(String.class, "id");
        if (!update.requestParameters().isEmpty()) {
            method.addParameter(ParameterSpec.builder(
                            ClassName.get("%s.application".formatted(polymorphic.packageName()),
                                    "%s%sRequest".formatted(leafName, capitalize(update.operationName()))), "request")
                    .addAnnotation(Valid.class)
                    .build());
        }
        method.addStatement("log.debug($S, $T.class.getSimpleName(), id)", "Updating {} with id: {}",
                polymorphic.className());
        var domainCallBlock = buildDomainCallBlock(update);
        method.addStatement("""
                        return $N.findById(id).map(shape -> {
                            if (!(shape instanceof $T aggregate)) {
                                throw new $T("Expected $L but got: " + shape.getClass().getSimpleName());
                            }
                            $L
                            return ($T) $N.save(aggregate);
                        })""",
                repositoryName,
                subtype.className(),
                IllegalStateException.class,
                leafName,
                domainCallBlock,
                polymorphic.className(),
                repositoryName);
        return method.build();
    }

    private static String leafName(String simpleName) {
        var dotIndex = simpleName.lastIndexOf('.');
        return dotIndex >= 0 ? simpleName.substring(dotIndex + 1) : simpleName;
    }
}
