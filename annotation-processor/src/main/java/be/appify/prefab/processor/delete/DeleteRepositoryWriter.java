package be.appify.prefab.processor.delete;

import com.palantir.javapoet.MethodSpec;

import javax.lang.model.element.Modifier;

public class DeleteRepositoryWriter {
    private static final MethodSpec DELETE_METHOD = MethodSpec.methodBuilder("deleteById")
            .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
            .addParameter(String.class, "id")
            .build();

    public MethodSpec deleteMethod() {
        return DELETE_METHOD;
    }
}
