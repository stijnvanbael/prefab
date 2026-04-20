package be.appify.prefab.processor.rest.create;

import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.processor.ClassManifest;
import be.appify.prefab.processor.PolymorphicAggregateManifest;
import be.appify.prefab.processor.PrefabContext;
import be.appify.prefab.processor.VariableManifest;
import com.palantir.javapoet.AnnotationSpec;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterSpec;
import com.palantir.javapoet.ParameterizedTypeName;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.lang.model.element.ExecutableElement;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;

import static be.appify.prefab.processor.CaseUtil.toKebabCase;
import static be.appify.prefab.processor.rest.ControllerUtil.operationAnnotation;
import static be.appify.prefab.processor.rest.ControllerUtil.requestMapping;
import static be.appify.prefab.processor.rest.ControllerUtil.securedAnnotation;
import static javax.lang.model.element.Modifier.PUBLIC;
import static org.apache.commons.text.WordUtils.capitalize;
import static org.atteo.evo.inflector.English.plural;

class CreateControllerWriter {
    MethodSpec createMethod(ClassManifest manifest, ExecutableElement constructor, PrefabContext context) {
        var create = constructor.getAnnotation(Create.class);
        var subtypeName = manifest.simpleName();
        return buildCreateMethod("create", subtypeName, manifest.packageName(), manifest.simpleName(),
                toKebabCase("/" + plural(manifest.simpleName()) + "/"), constructor, create, context);
    }

    MethodSpec createDispatchMethodForPolymorphic(
            PolymorphicAggregateManifest polymorphic,
            List<Map.Entry<ClassManifest, ExecutableElement>> group,
            PrefabContext context
    ) {
        var create = group.getFirst().getValue().getAnnotation(Create.class);
        var unionName = "Create%sRequest".formatted(polymorphic.simpleName());
        var unionClass = ClassName.get(polymorphic.packageName() + ".application", unionName);
        var redirectPath = toKebabCase("/" + plural(polymorphic.simpleName()) + "/");

        var switchCases = group.stream()
                .map(e -> buildDispatchCase(polymorphic, unionName, e))
                .collect(CodeBlock.joining(";\n"));

        var method = MethodSpec.methodBuilder("create")
                .addModifiers(PUBLIC)
                .addAnnotation(requestMapping(create.method(), create.path(), List.of()))
                .returns(ParameterizedTypeName.get(ResponseEntity.class, Void.class));
        operationAnnotation("Create a new " + polymorphic.simpleName()).ifPresent(method::addAnnotation);
        securedAnnotation(create.security()).ifPresent(method::addAnnotation);
        method.addParameter(ParameterSpec.builder(unionClass, "request")
                .addAnnotation(Valid.class)
                .addAnnotation(AnnotationSpec.builder(RequestBody.class).build())
                .build());
        method.addStatement("var id = switch (request) {\n$L;\n}", switchCases);
        method.addStatement("return $T.created($T.create($S + id)).build()",
                ResponseEntity.class, URI.class, redirectPath);
        return method.build();
    }

    private static CodeBlock buildDispatchCase(
            PolymorphicAggregateManifest polymorphic,
            String unionName,
            Map.Entry<ClassManifest, ExecutableElement> entry
    ) {
        var leafName = leafName(entry.getKey().simpleName());
        var nestedClass = ClassName.get(polymorphic.packageName() + ".application", unionName,
                "Create%sRequest".formatted(leafName));
        var flatClass = ClassName.get(polymorphic.packageName() + ".application",
                "Create%sRequest".formatted(leafName));
        var paramNames = entry.getValue().getParameters().stream()
                .map(p -> "r." + p.getSimpleName() + "()")
                .collect(Collectors.joining(", "));
        return CodeBlock.of("case $T r -> service.create$L(new $T($L))",
                nestedClass, leafName, flatClass, paramNames);
    }

    MethodSpec createMethodForPolymorphic(
            PolymorphicAggregateManifest polymorphic,
            ClassManifest subtype,
            ExecutableElement constructor,
            PrefabContext context
    ) {
        var create = constructor.getAnnotation(Create.class);
        var leafName = leafName(subtype.simpleName());
        var methodName = "create" + leafName;
        var redirectPath = toKebabCase("/" + plural(polymorphic.simpleName()) + "/");
        return buildCreateMethod(methodName, leafName, subtype.packageName(), leafName,
                redirectPath, constructor, create, context);
    }

    private MethodSpec buildCreateMethod(
            String methodName,
            String operationLabel,
            String packageName,
            String requestRecordPrefix,
            String redirectBasePath,
            ExecutableElement constructor,
            Create create,
            PrefabContext context
    ) {
        var requestParts = constructor.getParameters().stream()
                .flatMap(parameter -> context.requestParameterBuilder()
                        .buildMethodParameter(VariableManifest.of(parameter, context.processingEnvironment()))
                        .stream()).toList();
        var method = MethodSpec.methodBuilder(methodName)
                .addModifiers(PUBLIC)
                .addAnnotation(requestMapping(create.method(), create.path(), requestParts))
                .returns(ParameterizedTypeName.get(ResponseEntity.class, Void.class));
        operationAnnotation("Create a new " + operationLabel).ifPresent(method::addAnnotation);
        securedAnnotation(create.security()).ifPresent(method::addAnnotation);
        if (constructor.getParameters().isEmpty()) {
            method.addStatement("var id = service.$N()", methodName);
        } else {
            method.addParameter(ParameterSpec.builder(
                            ClassName.get("%s.application".formatted(packageName),
                                    "Create%sRequest".formatted(requestRecordPrefix)),
                            "request")
                    .addAnnotation(Valid.class)
                    .addAnnotation(requestParts.isEmpty()
                            ? AnnotationSpec.builder(RequestBody.class).build()
                            : AnnotationSpec.builder(RequestPart.class)
                                    .addMember("name", "$S", "body")
                                    .build())
                    .build());
            requestParts.forEach(method::addParameter);
            method.addStatement("var id = service.$N(request$L)", methodName,
                    requestParts.stream()
                            .map(param -> ".with%s(%s)".formatted(capitalize(param.name()), param.name()))
                            .collect(Collectors.joining(", ")));
        }
        return method
                .addStatement("return $T.created($T.create($S + id)).build()",
                        ResponseEntity.class, URI.class, redirectBasePath)
                .build();
    }

    private static String leafName(String simpleName) {
        var dotIndex = simpleName.lastIndexOf('.');
        return dotIndex >= 0 ? simpleName.substring(dotIndex + 1) : simpleName;
    }
}
