package be.appify.prefab.processor.update;

import be.appify.prefab.core.service.AggregateEnvelope;
import be.appify.prefab.core.service.Reference;
import be.appify.prefab.processor.ClassManifest;
import be.appify.prefab.processor.VariableManifest;
import com.palantir.javapoet.*;
import jakarta.validation.Valid;

import javax.lang.model.element.Modifier;
import java.util.Optional;

import static org.apache.commons.text.WordUtils.capitalize;
import static org.apache.commons.text.WordUtils.uncapitalize;

public class UpdateServiceWriter {
    public MethodSpec updateMethod(ClassManifest manifest, UpdateManifest update) {
        var method = MethodSpec.methodBuilder(update.operationName())
                .addModifiers(Modifier.PUBLIC)
                .returns(ParameterizedTypeName.get(ClassName.get(Optional.class), aggregateEnvelopeOf(manifest.type().asTypeName())))
                .addParameter(String.class, "id");
        if (!update.parameters().isEmpty()) {
            method.addParameter(ParameterSpec.builder(
                            ClassName.get("%s.application".formatted(manifest.packageName()), "%s%sRequest".formatted(
                                    manifest.simpleName(), capitalize(update.operationName()))), "request")
                    .addAnnotation(Valid.class)
                    .build());
        }
        var aggregateFunction = update.stateless()
                ? CodeBlock.of("""
                    aggregate.%s(%s);
                    return aggregate;
                """.formatted(update.operationName(),
                update.parameters().stream().map(this::fromRequest)
                        .collect(CodeBlock.joining(", "))))
                : CodeBlock.of("return aggregate.%s(%s);".formatted(update.operationName(),
                update.parameters().stream().map(this::fromRequest)
                        .collect(CodeBlock.joining(", "))));

        method.addStatement(
                "return %sRepository.getById(id).map(envelope -> %sRepository.save(envelope.map(aggregate -> { %n$L })))"
                        .formatted(uncapitalize(manifest.simpleName()), uncapitalize(manifest.simpleName())),
                aggregateFunction);
        return method.build();
    }

    private TypeName aggregateEnvelopeOf(TypeName typeName) {
        return ParameterizedTypeName.get(ClassName.get(AggregateEnvelope.class), typeName);
    }

    private CodeBlock fromRequest(VariableManifest param) {
        if (param.type().is(Reference.class)) {
            var type = param.type().parameters().getFirst().simpleName();
            return CodeBlock.of("toReference($S, %sRepository, request.%s())".formatted(
                    uncapitalize(type), param.name()), type);
        }
        return CodeBlock.of("request.%s()".formatted(param.name()));
    }
}
