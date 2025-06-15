package be.appify.prefab.processor.delete;

import com.palantir.javapoet.MethodSpec;

import javax.lang.model.element.Modifier;

public class DeleteRepositoryAdapterWriter {
    private static final MethodSpec DELETE_METHOD = MethodSpec.methodBuilder("deleteById")
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(Override.class)
            .addParameter(String.class, "id")
            .addStatement("repository.deleteById(id)")
            .build();

    public MethodSpec deleteMethod() {
        return DELETE_METHOD;
    }
}
