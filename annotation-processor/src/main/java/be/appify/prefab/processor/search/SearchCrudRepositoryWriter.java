package be.appify.prefab.processor.search;

import be.appify.prefab.processor.ClassManifest;
import be.appify.prefab.processor.VariableManifest;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterSpec;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeName;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import static org.apache.commons.lang3.StringUtils.capitalize;

import javax.lang.model.element.Modifier;
import java.util.Optional;

public class SearchCrudRepositoryWriter {
    public Optional<MethodSpec> searchMethod(ClassManifest manifest, VariableManifest searchProperty) {
        if (searchProperty != null) {
            var method = MethodSpec.methodBuilder("findBy%s%sLike".formatted(
                            manifest.parent().map(parent -> capitalize(parent.name()) + "And").orElse(""),
                            capitalize(searchProperty.name())))
                    .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                    .addParameter(searchProperty.asParameterSpec());
            manifest.parent().ifPresent(
                    parent -> method.addParameter(ParameterSpec.builder(String.class, parent.name()).build()));
            return Optional.of(method
                    .addParameter(ParameterSpec.builder(Pageable.class, "pageable").build())
                    .returns(pageOf(manifest.type().asTypeName()))
                    .build());
        }
        return Optional.empty();
    }

    private ParameterizedTypeName pageOf(TypeName typeName) {
        return ParameterizedTypeName.get(ClassName.get(Page.class), typeName);
    }
}
