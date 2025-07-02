package be.appify.prefab.processor.search;

import be.appify.prefab.core.service.AggregateEnvelope;
import be.appify.prefab.processor.ClassManifest;
import be.appify.prefab.processor.VariableManifest;
import be.appify.prefab.processor.spring.RepositorySupport;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterSpec;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeName;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import javax.lang.model.element.Modifier;

import static org.apache.commons.lang3.StringUtils.capitalize;

public class SearchRepositoryAdapterWriter {
    public MethodSpec searchMethod(ClassManifest manifest, VariableManifest searchProperty) {
        if (searchProperty != null) {
            var method = MethodSpec.methodBuilder("search")
                    .addModifiers(Modifier.PUBLIC)
                    .addAnnotation(Override.class)
                    .addParameter(Pageable.class, "pageable");
            manifest.parent().ifPresent(parent -> method.addParameter(ParameterSpec.builder(String.class, parent.name()).build()));
            return method
                    .addParameter(searchProperty.asParameterSpec())
                    .returns(pageOf(aggregatedEnvelopeOf(manifest.className())))
                    .addStatement(
                            "return $T.handleErrors(() -> ($N != null ? repository.$N($L, pageable) : repository.$N($L))\n" +
                                    ".map(data -> data.toAggregate(referenceProvider)))",
                            ClassName.get(RepositorySupport.class),
                            searchProperty.name(),
                            "findBy%sLike".formatted(manifest.parent().map(parent -> capitalize(parent.name()) + "And").orElse("")
                                    + capitalize(searchProperty.name())),
                            manifest.parent().map(parent -> parent.name() + ", ").orElse("") + searchProperty.name(),
                            "find%s".formatted(manifest.parent().map(parent -> "By" + capitalize(parent.name())).orElse("All")),
                            manifest.parent().map(parent -> parent.name() + ", ").orElse("") + "pageable"
                    )
                    .build();
        } else {
            var method = MethodSpec.methodBuilder("search")
                    .addModifiers(Modifier.PUBLIC)
                    .addAnnotation(Override.class)
                    .addParameter(Pageable.class, "pageable");
            manifest.parent().ifPresent(parent -> method.addParameter(ParameterSpec.builder(String.class, parent.name()).build()));
            return method
                    .returns(pageOf(aggregatedEnvelopeOf(manifest.className())))
                    .addStatement(
                            "return $T.handleErrors(() -> repository.$N($L).map(data -> data.toAggregate(referenceProvider)))",
                            ClassName.get(RepositorySupport.class),
                            "find%s".formatted(manifest.parent().map(parent -> "By" + capitalize(parent.name())).orElse("All")),
                            manifest.parent().map(parent -> parent.name() + ", ").orElse("") + "pageable"
                    )
                    .build();
        }
    }

    private ParameterizedTypeName pageOf(TypeName typeName) {
        return ParameterizedTypeName.get(ClassName.get(Page.class), typeName);
    }

    private ParameterizedTypeName aggregatedEnvelopeOf(TypeName typeName) {
        return ParameterizedTypeName.get(ClassName.get(AggregateEnvelope.class), typeName);
    }
}
