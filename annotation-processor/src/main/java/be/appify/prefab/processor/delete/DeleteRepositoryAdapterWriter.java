package be.appify.prefab.processor.delete;

import be.appify.prefab.processor.spring.RepositorySupport;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.MethodSpec;

import javax.lang.model.element.Modifier;

public class DeleteRepositoryAdapterWriter {
    private static final MethodSpec DELETE_METHOD = MethodSpec.methodBuilder("deleteById")
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(Override.class)
            .addParameter(String.class, "id")
            .addStatement("$T.handleErrors(() -> repository.deleteById(id))", ClassName.get(RepositorySupport.class))
            .build();

    public MethodSpec deleteMethod() {
        return DELETE_METHOD;
    }
}
