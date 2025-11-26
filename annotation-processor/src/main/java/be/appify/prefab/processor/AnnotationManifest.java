package be.appify.prefab.processor;

import com.palantir.javapoet.AnnotationSpec;
import com.palantir.javapoet.ClassName;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;

public class AnnotationManifest<A extends Annotation> {
    private final TypeManifest type;
    private final Map<String, AnnotationValue> values;
    private final A annotationValue;

    public AnnotationManifest(
            AnnotationMirror annotationMirror,
            ProcessingEnvironment processingEnvironment,
            A annotationValue
    ) {
        this.annotationValue = annotationValue;
        this.type = new TypeManifest(annotationMirror.getAnnotationType(), processingEnvironment);
        var elements = processingEnvironment.getElementUtils();
        this.values = elements.getElementValuesWithDefaults(annotationMirror).entrySet().stream()
                .collect(
                        HashMap::new,
                        (map, entry) -> map.put(entry.getKey().getSimpleName().toString(), entry.getValue()),
                        Map::putAll
                );
    }

    public TypeManifest type() {
        return type;
    }

    public AnnotationSpec asSpec() {
        var builder = AnnotationSpec.builder((ClassName) type.asTypeName());
        values.forEach((name, value) -> builder.addMember(name, value.toString()));
        return builder.build();
    }

    public A value() {
        return annotationValue;
    }
}
