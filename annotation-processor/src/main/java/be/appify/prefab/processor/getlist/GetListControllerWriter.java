package be.appify.prefab.processor.getlist;

import be.appify.prefab.core.annotations.rest.GetList;
import be.appify.prefab.processor.ClassManifest;
import be.appify.prefab.processor.VariableManifest;
import com.palantir.javapoet.AnnotationSpec;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterSpec;
import com.palantir.javapoet.ParameterizedTypeName;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.ArrayList;
import java.util.List;

import static be.appify.prefab.processor.ControllerUtil.securedAnnotation;
import static be.appify.prefab.processor.getlist.GetListUtil.filterPropertiesOf;
import static javax.lang.model.element.Modifier.PUBLIC;
import static org.apache.commons.text.WordUtils.uncapitalize;

public class GetListControllerWriter {

    public MethodSpec getListMethod(ClassManifest manifest, GetList getList) {
        var filters = filterPropertiesOf(manifest);

        var responseType = responseType(manifest);
        var method = MethodSpec.methodBuilder("getList")
                .addModifiers(PUBLIC)
                .addAnnotation(requestMapping(getList.method(), getList.path()))
                .returns(ParameterizedTypeName.get(
                        ClassName.get(ResponseEntity.class),
                        ParameterizedTypeName.get(
                                ClassName.get(PagedModel.class),
                                responseType)));
        securedAnnotation(getList.security()).ifPresent(method::addAnnotation);
        manifest.parent().ifPresent(parent -> method.addParameter(parentParameter(parent)));
        var parameters = getListParameters(manifest);
        method.addParameter(Pageable.class, "pageable");
        if (!filters.isEmpty()) {
            for (var filter : filters) {
                method.addParameter(ParameterSpec.builder(
                                ClassName.get(filter.field().type().packageName(), filter.field().type().simpleName()),
                                filter.field().name())
                        .build());
            }
            method.addStatement("return $T.ok(new $T(service.getList($L, $L).map($T::from)))",
                    ResponseEntity.class, PagedModel.class, parameters, String.join(", ",
                            filters.stream().map(filter -> filter.field().name()).toList()), responseType);
            return method.build();
        } else {
            return method.addStatement("return $T.ok(new $T(service.getList($L).map($T::from)))",
                            ResponseEntity.class, PagedModel.class, parameters, responseType)
                    .build();
        }
    }

    private ClassName responseType(ClassManifest manifest) {
        return ClassName.get("%s.infrastructure.http".formatted(manifest.packageName()),
                "%sResponse".formatted(manifest.simpleName()));
    }

    private ParameterSpec parentParameter(VariableManifest parent) {
        return ParameterSpec.builder(String.class, uncapitalize(parent.name()) + "Id")
                .addAnnotation(PathVariable.class)
                .build();
    }

    private static String getListParameters(ClassManifest manifest) {
        var parameters = new ArrayList<>(List.of("pageable"));
        manifest.parent().ifPresent(parent -> parameters.add(uncapitalize(parent.name()) + "Id"));
        return String.join(", ", parameters);
    }

    private AnnotationSpec requestMapping(String method, String path) {
        return AnnotationSpec.builder(RequestMapping.class)
                .addMember("method", "$T.$N", RequestMethod.class, method)
                .addMember("path", "$S", path)
                .build();
    }
}
