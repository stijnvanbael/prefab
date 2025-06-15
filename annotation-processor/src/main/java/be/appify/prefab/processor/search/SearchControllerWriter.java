package be.appify.prefab.processor.search;

import be.appify.prefab.core.annotations.rest.Search;
import be.appify.prefab.core.service.Reference;
import be.appify.prefab.processor.ClassManifest;
import be.appify.prefab.processor.VariableManifest;
import com.palantir.javapoet.*;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.ArrayList;
import java.util.List;

import static javax.lang.model.element.Modifier.PUBLIC;
import static org.apache.commons.text.WordUtils.uncapitalize;

public class SearchControllerWriter {
    public MethodSpec searchMethod(ClassManifest manifest, Search search) {
        var searchProperty = search.property().isBlank() ? null : manifest.fieldByName(search.property())
                .orElseThrow(() -> new IllegalArgumentException("Property %s.%s defined in Http.Search not found"
                        .formatted(manifest.qualifiedName(), search.property())));
        if (searchProperty != null && searchProperty.type().is(Reference.class)) {
            searchProperty = searchProperty.withType(String.class);
        }
        var responseType = responseType(manifest);
        var method = MethodSpec.methodBuilder("search")
                .addModifiers(PUBLIC)
                .addAnnotation(requestMapping(search.method(), search.path()))
                .returns(ParameterizedTypeName.get(
                        ClassName.get(ResponseEntity.class),
                        ParameterizedTypeName.get(
                                ClassName.get(PagedModel.class),
                                responseType)));
        manifest.parent().ifPresent(parent -> method.addParameter(parentParameter(parent)));
        var parameters = searchParameters(manifest);
        method.addParameter(Pageable.class, "pageable");
        if (searchProperty != null) {
            return method.addParameter(ParameterSpec.builder(
                                    ClassName.get(searchProperty.type().packageName(), searchProperty.type().simpleName()),
                                    searchProperty.name())
                            .build()).addStatement("return $T.ok(new $T(service.search($L, $N).map($T::from)))",
                            ResponseEntity.class, PagedModel.class, parameters, searchProperty.name(), responseType)
                    .build();
        } else {
            return method.addStatement("return $T.ok(new $T(service.search($L).map($T::from)))",
                            ResponseEntity.class, PagedModel.class, parameters, responseType)
                    .build();
        }
    }

    private ClassName responseType(ClassManifest manifest) {
        return ClassName.get("%s.infrastructure.http".formatted(manifest.packageName()),
                "%sResponse".formatted(manifest.simpleName()));
    }

    private ParameterSpec parentParameter(VariableManifest parent) {
        return ParameterSpec.builder(String.class, uncapitalize(parent.type().parameters().getFirst().simpleName()) + "Id")
                .addAnnotation(PathVariable.class)
                .build();
    }

    private static String searchParameters(ClassManifest manifest) {
        var parameters = new ArrayList<>(List.of("pageable"));
        manifest.parent().ifPresent(parent -> parameters.add(uncapitalize(parent.type().parameters().getFirst().simpleName()) + "Id"));
        return String.join(", ", parameters);
    }

    private AnnotationSpec requestMapping(String method, String path) {
        return AnnotationSpec.builder(RequestMapping.class)
                .addMember("method", "$T.$N", RequestMethod.class, method)
                .addMember("path", "$S", path)
                .build();
    }
}
