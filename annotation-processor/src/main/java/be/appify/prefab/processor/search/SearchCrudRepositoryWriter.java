package be.appify.prefab.processor.search;

import be.appify.prefab.processor.ClassManifest;
import be.appify.prefab.processor.TypeManifest;
import be.appify.prefab.processor.VariableManifest;
import com.palantir.javapoet.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import javax.lang.model.element.Modifier;
import java.util.Optional;

import static org.apache.commons.lang3.StringUtils.capitalize;

public class SearchCrudRepositoryWriter {
    public Optional<MethodSpec> searchMethod(ClassManifest manifest, VariableManifest searchProperty) {
        if (searchProperty != null) {
            var method = MethodSpec.methodBuilder("findBy%s%sLike".formatted(
                            manifest.parent().map(parent -> capitalize(parent.name()) + "And").orElse(""),
                            capitalize(searchProperty.name())))
                    .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                    .addParameter(searchProperty.asParameterSpec());
            manifest.parent().ifPresent(parent -> method.addParameter(ParameterSpec.builder(String.class, parent.name()).build()));
            return Optional.of(method
                    .addParameter(ParameterSpec.builder(Pageable.class, "pageable").build())
                    .returns(pageOf(dataType(manifest.type())))
                    .build());
        }
        return Optional.empty();
    }

    private ParameterizedTypeName pageOf(TypeName typeName) {
        return ParameterizedTypeName.get(ClassName.get(Page.class), typeName);
    }

    private ClassName dataType(TypeManifest manifest) {
        return ClassName.get("%s.infrastructure.persistence".formatted(manifest.packageName()),
                "%sData".formatted(manifest.simpleName()));
    }
}
