package be.appify.prefab.processor.rest.createorupdate;

import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.processor.ClassManifest;
import be.appify.prefab.processor.PrefabContext;
import be.appify.prefab.processor.VariableManifest;
import be.appify.prefab.processor.rest.ControllerUtil;
import be.appify.prefab.processor.rest.PathVariables;
import be.appify.prefab.processor.rest.create.CreateServiceWriter;
import com.palantir.javapoet.AnnotationSpec;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterSpec;
import com.palantir.javapoet.ParameterizedTypeName;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.stream.Collectors;
import javax.lang.model.element.Modifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import static be.appify.prefab.processor.CaseUtil.toKebabCase;
import static be.appify.prefab.processor.rest.ControllerUtil.operationAnnotation;
import static be.appify.prefab.processor.rest.ControllerUtil.requestMapping;
import static be.appify.prefab.processor.rest.ControllerUtil.securedAnnotation;
import static org.atteo.evo.inflector.English.plural;

class CreateOrUpdateControllerWriter {

    MethodSpec createOrUpdateMethod(ClassManifest manifest, CreateOrUpdateManifest createOrUpdate, PrefabContext context) {
        var create = createOrUpdate.createConstructor().getAnnotation(Create.class);
        var pathVarNames = PathVariables.extractFrom(create.path());
        var lookupVar = createOrUpdate.lookupVariable();
        var redirectPath = toKebabCase("/" + plural(manifest.simpleName()) + "/");

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
                .addAnnotation(requestMapping(create.method(), create.path(), java.util.List.of()))
                .returns(ParameterizedTypeName.get(ResponseEntity.class, Void.class));

        operationAnnotation("Create or update " + manifest.simpleName()).ifPresent(method::addAnnotation);
        securedAnnotation(create.security()).ifPresent(method::addAnnotation);

        var lookupParamSpec = ParameterSpec.builder(String.class, lookupVar)
                .addAnnotation(PathVariable.class);
        ControllerUtil.pathParameterAnnotation("The " + manifest.simpleName() + " " + lookupVar)
                .ifPresent(lookupParamSpec::addAnnotation);
        method.addParameter(lookupParamSpec.build());

        createOrUpdate.updateManifest().pathParameters().stream()
                .filter(p -> !p.name().equals(lookupVar))
                .forEach(p -> {
                    var paramSpec = ParameterSpec.builder(String.class, p.name())
                            .addAnnotation(PathVariable.class);
                    ControllerUtil.pathParameterAnnotation("The " + p.name())
                            .ifPresent(paramSpec::addAnnotation);
                    method.addParameter(paramSpec.build());
                });

        var serviceArgs = lookupVar;
        if (!bodyParams.isEmpty()) {
            var requestType = ClassName.get(
                    "%s.application".formatted(manifest.packageName()),
                    "Create%sRequest".formatted(manifest.simpleName()));
            method.addParameter(ParameterSpec.builder(requestType, "request")
                    .addAnnotation(Valid.class)
                    .addAnnotation(AnnotationSpec.builder(RequestBody.class).build())
                    .build());
            serviceArgs += ", request";
        }

        method.addStatement("var createdId = service.create($L)", serviceArgs);
        method.addStatement("return $T.created($T.create($S + createdId)).build()",
                ResponseEntity.class, URI.class, redirectPath);

        return method.build();
    }
}
