package be.appify.prefab.processor.rest.create;

import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.processor.ClassManifest;
import be.appify.prefab.processor.PrefabContext;
import be.appify.prefab.processor.VariableManifest;
import be.appify.prefab.processor.rest.ControllerUtil;
import be.appify.prefab.processor.rest.PathVariables;
import com.palantir.javapoet.AnnotationSpec;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterSpec;
import com.palantir.javapoet.ParameterizedTypeName;
import jakarta.validation.Valid;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.lang.model.element.ExecutableElement;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import static be.appify.prefab.processor.rest.ControllerUtil.operationAnnotation;
import static be.appify.prefab.processor.rest.ControllerUtil.requestMapping;
import static be.appify.prefab.processor.rest.ControllerUtil.securedAnnotation;
import static javax.lang.model.element.Modifier.PUBLIC;

/**
 * Generates the controller method for an async-commit {@code @Create} static factory.
 *
 * <p>The method delegates to the service and returns {@code 202 Accepted} with no body,
 * because the aggregate is not yet persisted at this point.
 */
class AsyncCreateControllerWriter {

    MethodSpec createMethod(ClassManifest manifest, ExecutableElement factoryMethod, PrefabContext context) {
        var create = factoryMethod.getAnnotation(Create.class);
        var pathVarNames = PathVariables.extractFrom(create.path());
        var params = factoryMethod.getParameters().stream()
                .map(p -> VariableManifest.of(p, context.processingEnvironment()))
                .toList();
        var bodyParams = AsyncCreateServiceWriter.nonParentNonPathParams(params, manifest, pathVarNames);
        var requestParts = params.stream()
                .flatMap(p -> context.requestParameterBuilder().buildMethodParameter(p).stream())
                .toList();
        var method = MethodSpec.methodBuilder("create")
                .addModifiers(PUBLIC)
                .addAnnotation(requestMapping(create.method(), create.path(), requestParts))
                .returns(ParameterizedTypeName.get(ResponseEntity.class, Void.class));
        operationAnnotation("Create " + manifest.simpleName() + " (async)").ifPresent(method::addAnnotation);
        securedAnnotation(create.security()).ifPresent(method::addAnnotation);
        addParameters(pathVarNames, params, method);
        var pathVarArgs = params.stream()
                .map(VariableManifest::name)
                .filter(pathVarNames::contains)
                .collect(Collectors.joining(", "));
        if (!bodyParams.isEmpty()) {
            method.addParameter(ParameterSpec.builder(
                            ClassName.get("%s.application".formatted(manifest.packageName()),
                                    "Create%sRequest".formatted(manifest.simpleName())), "request")
                    .addAnnotation(Valid.class)
                    .addAnnotation(AnnotationSpec.builder(RequestBody.class).build())
                    .build());
            var serviceArgs = pathVarArgs.isBlank() ? "request" : pathVarArgs + ", request";
            method.addStatement("service.create($L)", serviceArgs);
        } else if (!pathVarArgs.isBlank()) {
            method.addStatement("service.create($L)", pathVarArgs);
        } else {
            method.addStatement("service.create()");
        }
        method.addStatement("return $T.accepted().build()", ResponseEntity.class);
        return method.build();
    }

    static void addParameters(Set<String> pathVarNames, List<VariableManifest> params, MethodSpec.Builder method) {
        params.stream()
                .filter(p -> pathVarNames.contains(p.name()))
                .forEach(p -> {
                    var spec = ParameterSpec.builder(String.class, p.name())
                            .addAnnotation(PathVariable.class);
                    ControllerUtil.pathParameterAnnotation("The " + p.name()).ifPresent(spec::addAnnotation);
                    method.addParameter(spec.build());
                });
    }
}
