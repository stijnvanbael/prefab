package be.appify.prefab.processor;

import com.palantir.javapoet.AnnotationSpec;
import com.palantir.javapoet.ClassName;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a manifest of an annotation, including its type and values.
 * @param <A> The type of the annotation.
 */
public class AnnotationManifest<A extends Annotation> {
    private final TypeManifest type;
    private final Map<String, AnnotationValue> values;
    private final A annotationValue;

    AnnotationManifest(
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

    /**
     * Returns the type manifest of the annotation.
     *
     * @return The type manifest of the annotation.
     */
    public TypeManifest type() {
        return type;
    }

    /**
     * Converts the annotation manifest to an AnnotationSpec.
     * @return The values of the annotation as an AnnotationSpec.
     */
    public AnnotationSpec asSpec() {
        var builder = AnnotationSpec.builder((ClassName) type.asTypeName());
        values.forEach((name, value) -> builder.addMember(name, value.toString()));
        return builder.build();
    }

    /**
     * Returns the annotation instance represented by this manifest.
     *
     * @return The annotation instance represented by this manifest.
     */
    public A value() {
        return annotationValue;
    }
}
