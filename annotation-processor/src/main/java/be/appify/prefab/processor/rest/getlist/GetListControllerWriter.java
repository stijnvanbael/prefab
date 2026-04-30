package be.appify.prefab.processor.rest.getlist;

import be.appify.prefab.core.annotations.rest.GetList;
import be.appify.prefab.processor.ClassManifest;
import be.appify.prefab.processor.PolymorphicAggregateManifest;
import be.appify.prefab.processor.VariableManifest;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterSpec;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeName;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.ArrayList;
import java.util.List;

import static be.appify.prefab.processor.rest.ControllerUtil.operationAnnotation;
import static be.appify.prefab.processor.rest.ControllerUtil.requestMapping;
import static be.appify.prefab.processor.rest.ControllerUtil.responseType;
import static be.appify.prefab.processor.rest.ControllerUtil.securedAnnotation;
import static be.appify.prefab.processor.rest.getlist.GetListUtil.filterPropertiesOf;
import static javax.lang.model.element.Modifier.PUBLIC;
import static org.apache.commons.text.WordUtils.uncapitalize;
import static org.atteo.evo.inflector.English.plural;

class GetListControllerWriter {

    MethodSpec getListMethod(ClassManifest manifest, GetList getList) {
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
        operationAnnotation("List " + plural(manifest.simpleName())).ifPresent(method::addAnnotation);
        securedAnnotation(getList.security()).ifPresent(method::addAnnotation);
        manifest.parent().ifPresent(parent -> method.addParameter(parentParameter(parent)));
        var parameters = getListParameters(manifest);
        method.addParameter(Pageable.class, "pageable");
        if (!filters.isEmpty()) {
            for (var filter : filters) {
                method.addParameter(ParameterSpec.builder(
                                filterParamType(filter.field()),
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

    private ParameterSpec parentParameter(VariableManifest parent) {
        return ParameterSpec.builder(String.class, uncapitalize(parent.name()) + "Id")
                .addAnnotation(PathVariable.class)
                .build();
    }

    private static TypeName filterParamType(VariableManifest field) {
        return field.type().isSingleValueType()
                ? field.type().fields().getFirst().type().asBoxed().asTypeName()
                : ClassName.get(field.type().packageName(), field.type().simpleName());
    }

    private static String getListParameters(ClassManifest manifest) {
        var parameters = new ArrayList<>(List.of("pageable"));
        manifest.parent().ifPresent(parent -> parameters.add(uncapitalize(parent.name()) + "Id"));
        return String.join(", ", parameters);
    }

    MethodSpec getListMethod(PolymorphicAggregateManifest manifest, GetList getList) {
        var responseType = responseType(manifest);
        var method = MethodSpec.methodBuilder("getList")
                .addModifiers(PUBLIC)
                .addAnnotation(requestMapping(getList.method(), getList.path()))
                .returns(ParameterizedTypeName.get(
                        ClassName.get(ResponseEntity.class),
                        ParameterizedTypeName.get(ClassName.get(PagedModel.class), responseType)));
        operationAnnotation("List " + plural(manifest.simpleName())).ifPresent(method::addAnnotation);
        securedAnnotation(getList.security()).ifPresent(method::addAnnotation);
        manifest.parent().ifPresent(parent -> method.addParameter(parentParameter(parent)));
        method.addParameter(Pageable.class, "pageable");
        var serviceArgs = manifest.parent()
                .map(parent -> uncapitalize(parent.name()) + "Id, pageable")
                .orElse("pageable");
        return method
                .addStatement("return $T.ok(new $T(service.getList($L).map($T::from)))",
                        ResponseEntity.class, PagedModel.class, serviceArgs, responseType)
                .build();
    }
}
