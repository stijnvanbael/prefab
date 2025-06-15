package be.appify.prefab.processor;

import com.palantir.javapoet.AnnotationSpec;
import com.palantir.javapoet.ClassName;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import java.util.HashMap;
import java.util.Map;

public class AnnotationManifest {
    private final String expression;
    private final TypeManifest type;
    private final Map<String, AnnotationValue> values;

    public AnnotationManifest(AnnotationMirror annotationMirror, ProcessingEnvironment processingEnvironment) {
        this.expression = annotationMirror.toString();
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

    public Object value(String name) {
        return values.get(name).getValue();
    }
}
