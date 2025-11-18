package be.appify.prefab.processor.getlist;

import be.appify.prefab.core.annotations.rest.Filter;
import be.appify.prefab.core.annotations.rest.GetList;
import be.appify.prefab.core.service.Reference;
import be.appify.prefab.processor.ClassManifest;
import be.appify.prefab.processor.ControllerUtil;
import be.appify.prefab.processor.VariableManifest;
import be.appify.prefab.processor.spring.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterizedTypeName;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import javax.lang.model.element.Modifier;
import java.util.List;

import static org.atteo.evo.inflector.English.plural;

public class GetListTestFixtureWriter {
    public MethodSpec getListMethod(ClassManifest manifest) {
        var search = manifest.annotationsOfType(GetList.class).stream().findFirst().orElseThrow();
        var filterProperties = manifest.fields().stream()
                .filter(field -> field.hasAnnotation(Filter.class))
                .map(field -> field.type().is(Reference.class) ? field.withType(String.class) : field)
                .toList();
        var returnType = returnType(manifest);
        var method = methodSignature(manifest, returnType, filterProperties);
        createRequest(manifest, method, search);
        addPaging(method);
        addSorting(method);
        for (var property : filterProperties) {
            addFilterParam(property.name(), method);
        }
        return performRequest(method, returnType);
    }

    private static ParameterizedTypeName returnType(ClassManifest manifest) {
        return ParameterizedTypeName.get(ClassName.get(Page.class),
                ClassName.get(manifest.packageName() + ".infrastructure.http",
                        "%sResponse".formatted(manifest.simpleName())));
    }

    private static MethodSpec.Builder methodSignature(
            ClassManifest manifest,
            ParameterizedTypeName returnType,
            List<VariableManifest> filterProperties
    ) {
        var method = MethodSpec.methodBuilder("find" + plural(manifest.simpleName()))
                .addModifiers(Modifier.PUBLIC)
                .returns(returnType)
                .addParameter(Pageable.class, "pageable");

        for (VariableManifest filterProperty : filterProperties) {
            method.addParameter(
                    ClassName.get(filterProperty.type().packageName(), filterProperty.type().simpleName()),
                    filterProperty.name());
        }
        manifest.parent().ifPresent(parent -> method.addParameter(String.class, parent.name()));
        method.addException(Exception.class);
        return method;
    }

    private static void createRequest(ClassManifest manifest, MethodSpec.Builder method, GetList getList) {
        manifest.parent().ifPresentOrElse(
                parent -> method.addStatement("var request = $T.$N($S, $L)",
                        MockMvcRequestBuilders.class,
                        getList.method().toLowerCase(),
                        "/" + ControllerUtil.pathOf(manifest) + getList.path(),
                        parent.name()),
                () -> method.addStatement("var request = $T.$N($S)",
                        MockMvcRequestBuilders.class,
                        getList.method().toLowerCase(),
                        "/" + ControllerUtil.pathOf(manifest) + getList.path()));
    }

    private static void addPaging(MethodSpec.Builder method) {
        method.addCode("""
                if (pageable != null && pageable.isPaged()) {
                    request.queryParam("page", String.valueOf(pageable.getPageNumber()))
                           .queryParam("size", String.valueOf(pageable.getPageSize()));
                }
                """);
    }

    private static void addSorting(MethodSpec.Builder method) {
        method.addCode("""
                        if (pageable != null && pageable.getSort().isSorted()) {
                            request.queryParam("sort", $T.toRequestParams(pageable.getSort()));
                        }
                        """,
                ControllerUtil.class);
    }

    private static void addFilterParam(String property, MethodSpec.Builder method) {
        if (property != null) {
            method.addCode("""
                    if ($N != null) {
                        request.queryParam($S, $N);
                    }
                    """, property, property, property);
        }
    }

    private static MethodSpec performRequest(MethodSpec.Builder method, ParameterizedTypeName returnType) {
        return method.addStatement("""
                                var json = mockMvc.perform(request.accept($T.APPLICATION_JSON))
                                        .andExpect($T.status().isOk())
                                        .andReturn()
                                        .getResponse()
                                        .getContentAsString()""",
                        MediaType.class,
                        MockMvcResultMatchers.class
                )
                .addStatement("return objectMapper.readValue(json, new $T() {})",
                        ParameterizedTypeName.get(ClassName.get(TypeReference.class), returnType))
                .build();
    }
}
