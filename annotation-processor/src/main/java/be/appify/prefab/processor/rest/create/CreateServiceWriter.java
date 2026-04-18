package be.appify.prefab.processor.rest.create;

import be.appify.prefab.processor.ClassManifest;
import be.appify.prefab.processor.PrefabContext;
import be.appify.prefab.processor.VariableManifest;
import be.appify.prefab.processor.audit.AuditFields;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterSpec;
import jakarta.validation.Valid;
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
        if (constructor.getParameters().isEmpty()) {
            method.addStatement("var aggregate = new $T()", manifest.type().asTypeName());
        } else {
            method.addParameter(ParameterSpec.builder(
                                    ClassName.get("%s.application".formatted(manifest.packageName()),
                                            "Create%sRequest".formatted(manifest.simpleName())), "request")
                            .addAnnotation(Valid.class)
                            .build())
                    .addStatement("var aggregate = new $T($L)", manifest.type().asTypeName(),
                            constructor.getParameters().stream()
                                    .map(param -> VariableManifest.of(param, context.processingEnvironment()))
                                    .map(context.requestParameterMapper()::mapRequestParameter)
                                    .collect(CodeBlock.joining(", ")));
        }
        tenantField.ifPresent(tf -> method.addStatement("aggregate = new $T($L)",
                manifest.type().asTypeName(), reconstructionArgs(manifest, tf)));
        addAuditForCreate(method, manifest);
        return saveAndReturnId(method, manifest);
    }

    private static MethodSpec saveAndReturnId(MethodSpec.Builder method, ClassManifest manifest) {
        var idField = manifest.idField();
        var idName = idField.map(VariableManifest::name).orElse("id");
        var idSuffix = idField.filter(f -> f.type().isSingleValueType())
                .map(f -> ".%s()".formatted(f.type().singleValueAccessor()))
                .orElse("");
        return method
                .addStatement("%sRepository.save(aggregate)".formatted(uncapitalize(manifest.simpleName())))
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
}
