package be.appify.prefab.processor;

import com.palantir.javapoet.TypeSpec;
import javax.lang.model.element.TypeElement;

interface TestFileOutput {
    void setPreferredElement(TypeElement element);

    void writeFile(String packagePrefix, String typeName, TypeSpec type);
}
