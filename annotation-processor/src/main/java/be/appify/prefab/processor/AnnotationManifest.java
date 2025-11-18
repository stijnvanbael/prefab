package be.appify.prefab.processor;

import com.palantir.javapoet.AnnotationSpec;
import com.palantir.javapoet.ClassName;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.VariableElement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class AnnotationManifest {
    private final String expression;
    private final TypeManifest type;
    private final Map<String, AnnotationValue> values;
    private final ProcessingEnvironment processingEnvironment;

    public AnnotationManifest(AnnotationMirror annotationMirror, ProcessingEnvironment processingEnvironment) {
        this.expression = annotationMirror.toString();
        this.processingEnvironment = processingEnvironment;
        this.type = new TypeManifest(annotationMirror.getAnnotationType(), processingEnvironment);
        var elements = processingEnvironment.getElementUtils();
        this.values = elements.getElementValuesWithDefaults(annotationMirror).entrySet().stream()
                .collect(
                        HashMap::new,
                        (map, entry) -> map.put(entry.getKey().getSimpleName().toString(), entry.getValue()),
                        Map::putAll
                );
    }

    public String expression() {
        return expression;
    }

    public TypeManifest type() {
        return type;
    }

    public AnnotationSpec asSpec() {
        var builder = AnnotationSpec.builder((ClassName) type.asTypeName());
        values.forEach((name, value) -> builder.addMember(name, value.toString()));
        return builder.build();
    }

    @SuppressWarnings("unchecked")
    public Object value(String name) {
        var value = values.get(name).getValue();
        if (value instanceof List<?> list) {
            if (!list.isEmpty() && list.getFirst() instanceof AnnotationMirror) {
                return list.stream()
                        .map(item -> new AnnotationManifest((AnnotationMirror) item, processingEnvironment))
                        .toList();
            }
        } else if (value instanceof VariableElement variable) {
            return Enum.valueOf(
                    Stream.of(type.asClass().getMethods())
                            .filter(field -> name.equals(field.getName()))
                            .findFirst()
                            .map(property -> (Class<Enum>) property.getReturnType())
                            .orElseThrow(),
                    variable.getSimpleName().toString());
        }
        return value;
    }
}
