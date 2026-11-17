package be.appify.prefab.processor;

import com.palantir.javapoet.TypeSpec;
import javax.lang.model.element.TypeElement;

/**
 * Abstraction for writing generated Java types to a concrete output destination.
 */
public interface FileOutput {
    default void setPreferredElement(TypeElement element) {
        // Most outputs do not resolve filesystem roots and can ignore this hint.
    }

    /**
     * Associates subsequently generated files with the model types that contributed to them.
     *
     * @param elements source model types that contribute to the generated files
     */
    default void setOriginatingElements(TypeElement... elements) {
        if (elements.length > 0) {
            setPreferredElement(elements[0]);
        }
    }

    void writeFile(String packagePrefix, String typeName, TypeSpec type);
}
