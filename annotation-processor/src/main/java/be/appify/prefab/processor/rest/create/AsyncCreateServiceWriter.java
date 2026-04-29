package be.appify.prefab.processor.rest.create;

import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.processor.ClassManifest;
import be.appify.prefab.processor.PrefabContext;
import be.appify.prefab.processor.VariableManifest;
import be.appify.prefab.processor.rest.PathVariables;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterSpec;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;

/**
 * Generates the service method for an async-commit {@code @Create} static factory.
 *
 * <p>The generated method calls the static factory and returns {@code void} so the
 * controller can respond with {@code 202 Accepted}. The aggregate root is responsible
 * for publishing any domain events itself.
 */
class AsyncCreateServiceWriter {

    MethodSpec createMethod(ClassManifest manifest, ExecutableElement factoryMethod, PrefabContext context) {
        var method = MethodSpec.methodBuilder("create")
                .addModifiers(Modifier.PUBLIC)
                .returns(void.class)
                .addStatement("log.debug($S, $T.class.getSimpleName())", "Async-creating new {}", manifest.className());
        addFactoryArgs(method, manifest, factoryMethod, context);
        return method.build();
    }

    private void addFactoryArgs(
            MethodSpec.Builder method,
            ClassManifest manifest,
            ExecutableElement factoryMethod,
            PrefabContext context
    ) {
        var create = factoryMethod.getAnnotation(Create.class);
        var pathVarNames = PathVariables.extractFrom(create != null ? create.path() : "");
        var params = factoryMethod.getParameters().stream()
                .map(p -> VariableManifest.of(p, context.processingEnvironment()))
                .toList();
        if (params.isEmpty()) {
            method.addStatement("$T.$N()", manifest.type().asTypeName(), factoryMethod.getSimpleName());
            return;
        }
        params.stream()
                .filter(p -> pathVarNames.contains(p.name()))
                .forEach(p -> method.addParameter(String.class, p.name()));
        var bodyParams = nonParentNonPathParams(params, manifest, pathVarNames);
        if (!bodyParams.isEmpty()) {
            method.addParameter(ParameterSpec.builder(
                            ClassName.get("%s.application".formatted(manifest.packageName()),
                                    "Create%sRequest".formatted(manifest.simpleName())), "request")
                    .addAnnotation(Valid.class)
                    .build());
        }
        var constructorArgs = params.stream()
                .map(p -> resolveParam(p, pathVarNames, context))
                .collect(CodeBlock.joining(", "));
        method.addStatement("$T.$N($L)", manifest.type().asTypeName(), factoryMethod.getSimpleName(), constructorArgs);
    }

    private static CodeBlock resolveParam(VariableManifest param, Set<String> pathVarNames, PrefabContext context) {
        if (pathVarNames.contains(param.name())) {
            return CodeBlock.of("$N", param.name());
        }
        return context.requestParameterMapper().mapRequestParameter(param);
    }

    static List<VariableManifest> nonParentNonPathParams(
            List<VariableManifest> params,
            ClassManifest manifest,
            Set<String> pathVarNames
    ) {
        var parentName = manifest.parent()
                .filter(p -> !p.type().parameters().isEmpty())
                .map(VariableManifest::name);
        return params.stream()
                .filter(p -> parentName.map(n -> !n.equals(p.name())).orElse(true))
                .filter(p -> !pathVarNames.contains(p.name()))
                .toList();
    }
}
