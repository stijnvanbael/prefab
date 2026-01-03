package be.appify.prefab.processor.getlist;

import be.appify.prefab.processor.ClassManifest;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterSpec;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeName;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import javax.lang.model.element.Modifier;
import java.util.Optional;

import static be.appify.prefab.processor.getlist.GetListUtil.filterPropertiesOf;

class GetListRepositoryWriter {
    Optional<MethodSpec> getListMethod(ClassManifest manifest) {
        var filterProperties = filterPropertiesOf(manifest);
        if (!filterProperties.isEmpty()) {
            var method = MethodSpec.methodBuilder("findAll")
                    .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                    .addParameter(ParameterSpec.builder(
                                    ParameterizedTypeName.get(ClassName.get(Example.class), manifest.type().asTypeName()),
                                    "example")
                            .build());
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
