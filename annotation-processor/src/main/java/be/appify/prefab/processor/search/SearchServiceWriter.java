package be.appify.prefab.processor.search;

import be.appify.prefab.core.service.AggregateEnvelope;
import be.appify.prefab.processor.ClassManifest;
import be.appify.prefab.processor.VariableManifest;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeName;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import javax.lang.model.element.Modifier;

import static org.apache.commons.text.WordUtils.uncapitalize;

public class SearchServiceWriter {
    public MethodSpec searchMethod(ClassManifest manifest, VariableManifest searchProperty) {
        var method = MethodSpec.methodBuilder("search")
                .addModifiers(Modifier.PUBLIC)
                .addParameter(Pageable.class, "pageable");
        manifest.parent().ifPresent(parent -> method.addParameter(String.class, uncapitalize(parent.type().parameters().getFirst().simpleName()) + "Id"));
        if (searchProperty != null) {
            method.addParameter(searchProperty.type().asTypeName(), searchProperty.name());
        }
        TypeName typeName = manifest.type().asTypeName();
        return method
                .returns(ParameterizedTypeName.get(ClassName.get(Page.class),
                        ParameterizedTypeName.get(ClassName.get(AggregateEnvelope.class), typeName)))
                .addStatement("return $N.search($L)",
                        uncapitalize(manifest.simpleName()) + "Repository",
                        "pageable"
                                + (manifest.parent().map(parent -> ", %sId".formatted(uncapitalize(parent.type().parameters().getFirst().simpleName()))).orElse(""))
                                + (searchProperty == null ? "" : ", %s".formatted(searchProperty.name())))
                .build();
    }
}
