package be.appify.prefab.processor.rest.create;

import be.appify.prefab.core.tenant.TenantContextProvider;
import be.appify.prefab.processor.ClassManifest;
import be.appify.prefab.processor.PrefabContext;
import be.appify.prefab.processor.VariableManifest;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterSpec;
import jakarta.validation.Valid;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;

import static org.apache.commons.text.WordUtils.uncapitalize;

class CreateServiceWriter {
    MethodSpec createMethod(ClassManifest manifest, ExecutableElement controller, PrefabContext context) {
        var tenantField = manifest.tenantIdField();
        if (controller.getParameters().isEmpty()) {
            var method = MethodSpec.methodBuilder("create")
                    .addModifiers(Modifier.PUBLIC)
                    .returns(String.class)
                    .addStatement("log.debug($S, $T.class.getSimpleName())", "Creating new {}", manifest.className())
                    .addStatement("var aggregate = new $T()", manifest.type().asTypeName());
            tenantField.ifPresent(tf -> method.addStatement("aggregate = new $T($L)",
                    manifest.type().asTypeName(), reconstructionArgs(manifest, tf)));
            method.addStatement("%sRepository.save(aggregate)".formatted(uncapitalize(manifest.simpleName())))
                    .addStatement("return aggregate.$N()$L",
                            manifest.idField().map(VariableManifest::name).orElse("id"),
                            manifest.idField().map(VariableManifest::type).map(type -> type.isSingleValueType() ? ".%s()".formatted(type.singleValueAccessor()) : "").orElse(""));
            return method.build();
        } else {
            var method = MethodSpec.methodBuilder("create")
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(ParameterSpec.builder(
                                    ClassName.get("%s.application".formatted(manifest.packageName()),
                                            "Create%sRequest".formatted(manifest.simpleName())), "request")
                            .addAnnotation(Valid.class)
                            .build())
                    .returns(String.class)
                    .addStatement("log.debug($S, $T.class.getSimpleName())", "Creating new {}", manifest.className())
                    .addStatement("var aggregate = new $T($L)", manifest.type().asTypeName(),
                            controller.getParameters().stream()
                                    .map(param -> VariableManifest.of(param, context.processingEnvironment()))
                                    .map(context.requestParameterMapper()::mapRequestParameter)
                                    .collect(CodeBlock.joining(", ")));
            tenantField.ifPresent(tf -> method.addStatement("aggregate = new $T($L)",
                    manifest.type().asTypeName(), reconstructionArgs(manifest, tf)));
            method.addStatement("%sRepository.save(aggregate)".formatted(uncapitalize(manifest.simpleName())))
                    .addStatement("return aggregate.$N()$L",
                            manifest.idField().map(VariableManifest::name).orElse("id"),
                            manifest.idField().map(VariableManifest::type).map(type -> type.isSingleValueType() ? ".%s()".formatted(type.singleValueAccessor()) : "").orElse(""));
            return method.build();
        }
    }

    /**
     * Builds a CodeBlock of comma-separated constructor arguments for reconstructing the aggregate with the
     * tenant ID field populated from {@link TenantContextProvider#currentTenantId()}.
     */
    private static CodeBlock reconstructionArgs(ClassManifest manifest, VariableManifest tenantField) {
        return manifest.fields().stream()
                .map(field -> field.name().equals(tenantField.name())
                        ? CodeBlock.of("tenantContextProvider.currentTenantId()")
                        : CodeBlock.of("aggregate.$N()", field.name()))
                .collect(CodeBlock.joining(", "));
    }
}
