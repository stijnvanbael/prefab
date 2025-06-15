package be.appify.prefab.processor.search;

import be.appify.prefab.core.service.AggregateEnvelope;
import be.appify.prefab.processor.ClassManifest;
import be.appify.prefab.processor.VariableManifest;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterizedTypeName;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import javax.lang.model.element.Modifier;

public class SearchRepositoryWriter {
    public MethodSpec searchMethod(ClassManifest manifest, VariableManifest searchProperty) {
        var aggregateType = manifest.type().asTypeName();
        var method = MethodSpec.methodBuilder("search")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addParameter(Pageable.class, "pageable");
        manifest.parent().ifPresent(parent -> method.addParameter(String.class, parent.name()));
        if (searchProperty != null) {
            method.addParameter(searchProperty.type().asTypeName(), searchProperty.name());
        }
        return method
                .returns(ParameterizedTypeName.get(ClassName.get(Page.class),
                        ParameterizedTypeName.get(ClassName.get(AggregateEnvelope.class), aggregateType)))
                .build();
    }
}
