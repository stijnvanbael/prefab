package be.appify.prefab.processor.update;

import be.appify.prefab.core.service.Reference;
import be.appify.prefab.processor.ClassManifest;
import be.appify.prefab.processor.VariableManifest;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterSpec;
import com.palantir.javapoet.ParameterizedTypeName;
import jakarta.validation.Valid;
import static org.apache.commons.text.WordUtils.capitalize;
import static org.apache.commons.text.WordUtils.uncapitalize;

import javax.lang.model.element.Modifier;
import java.util.Optional;

class UpdateServiceWriter {
    MethodSpec updateMethod(ClassManifest manifest, UpdateManifest update) {
        var method = MethodSpec.methodBuilder(update.operationName())
                .addModifiers(Modifier.PUBLIC)
                .returns(ParameterizedTypeName.get(ClassName.get(Optional.class), manifest.type().asTypeName()))
                .addParameter(String.class, "id");
        if (!update.parameters().isEmpty()) {
            method.addParameter(ParameterSpec.builder(
                            ClassName.get("%s.application".formatted(manifest.packageName()), "%s%sRequest".formatted(
                                    manifest.simpleName(), capitalize(update.operationName()))), "request")
                    .addAnnotation(Valid.class)
                    .build());
        }
        method.addStatement("log.debug($S, $T.class.getSimpleName(), id)", "Updating {} with id: {}",
                manifest.className());
        var aggregateFunction = update.stateful()
                ? CodeBlock.of("aggregate.%s(%s);"
                .formatted(update.operationName(),
                        update.parameters().stream().map(this::fromRequest)
                                .collect(CodeBlock.joining(", "))))
                : CodeBlock.of("aggregate = aggregate.%s(%s);".formatted(update.operationName(),
                        update.parameters().stream().map(this::fromRequest)
                                .collect(CodeBlock.joining(", "))));
        var repositoryName = uncapitalize(manifest.simpleName()) + "Repository";
        method.addStatement("""
                        return $N.findById(id).map(aggregate -> {
                            $L
                            return $N.save(aggregate);
                        })""",
                repositoryName,
                aggregateFunction,
                repositoryName);
        return method.build();
    }

    private CodeBlock fromRequest(VariableManifest parameter) {
        if (parameter.type().is(Reference.class)) {
            var type = parameter.type().parameters().getFirst().asTypeName();
            return CodeBlock.of("referenceFactory.referenceTo($T.class, request.$N())", type, parameter.name());
        }
        return CodeBlock.of("request.%s()".formatted(parameter.name()));
    }
}
