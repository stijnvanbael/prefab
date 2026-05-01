package be.appify.prefab.processor.rest.getbyid;

import be.appify.prefab.core.annotations.rest.GetById;
import be.appify.prefab.processor.ClassManifest;
import be.appify.prefab.processor.PolymorphicAggregateManifest;
import be.appify.prefab.processor.rest.ControllerUtil;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeName;
import javax.lang.model.element.Modifier;
import org.springframework.http.MediaType;

import static be.appify.prefab.processor.TestClasses.MOCK_MVC_REQUEST_BUILDERS;
import static be.appify.prefab.processor.TestClasses.MOCK_MVC_RESULT_MATCHERS;
import static be.appify.prefab.processor.TestClasses.REST_RESPONSE_ASSERT;

class GetByIdTestClientWriter {
    MethodSpec getByIdMethod(ClassManifest manifest) {
        var getById = manifest.annotationsOfType(GetById.class).stream().findFirst().orElseThrow();
        var responseType = ClassName.get(manifest.packageName() + ".infrastructure.http",
                "%sResponse".formatted(manifest.simpleName()));
        var returnType = ParameterizedTypeName.get(REST_RESPONSE_ASSERT, responseType);
        var method = MethodSpec.methodBuilder("get" + manifest.simpleName() + "ById")
                .addModifiers(Modifier.PUBLIC)
                .returns(returnType)
                .addException(Exception.class)
                .addParameter(String.class, "id");
        manifest.parent().ifPresent(parent -> method.addParameter(String.class, parent.name()));
        return method
                .addStatement("""
                                var result = mockMvc.perform($T.$N($S, $L)$L
                                                .accept($T.APPLICATION_JSON))
                                        .andExpect($T.status().isOk())""",
                        MOCK_MVC_REQUEST_BUILDERS,
                        getById.method().toLowerCase(),
                        "/" + ControllerUtil.pathOf(manifest) + getById.path(),
                        manifest.parent().map(parent -> parent.name() + ", ").orElse("") + "id",
                        ControllerUtil.withMockUser(getById.security()),
                        MediaType.class,
                        MOCK_MVC_RESULT_MATCHERS)
                .addStatement("var json = result.andReturn().getResponse().getContentAsString()")
                .addStatement("return new $T<>(result, null, jsonMapper.readValue(json, $T.class))",
                        REST_RESPONSE_ASSERT, responseType)
                .build();
    }

    MethodSpec getByIdMethodForPolymorphic(PolymorphicAggregateManifest manifest) {
        var getById = manifest.annotationsOfType(GetById.class).stream().findFirst().orElseThrow();
        var responseType = ControllerUtil.responseType(manifest);
        var returnType = ParameterizedTypeName.get(REST_RESPONSE_ASSERT, responseType);
        var method = MethodSpec.methodBuilder("get" + manifest.simpleName() + "ById")
                .addModifiers(Modifier.PUBLIC)
                .returns(returnType)
                .addException(Exception.class)
                .addParameter(String.class, "id");
        manifest.parent().ifPresent(parent -> method.addParameter(String.class, parent.name()));
        return method
                .addStatement("""
                                var result = mockMvc.perform($T.$N($S, $L)$L
                                                .accept($T.APPLICATION_JSON))
                                        .andExpect($T.status().isOk())""",
                        MOCK_MVC_REQUEST_BUILDERS,
                        getById.method().toLowerCase(),
                        "/" + ControllerUtil.pathOf(manifest) + getById.path(),
                        manifest.parent().map(parent -> parent.name() + ", ").orElse("") + "id",
                        ControllerUtil.withMockUser(getById.security()),
                        MediaType.class,
                        MOCK_MVC_RESULT_MATCHERS)
                .addStatement("var json = result.andReturn().getResponse().getContentAsString()")
                .addStatement("return new $T<>(result, null, jsonMapper.readValue(json, $T.class))",
                        REST_RESPONSE_ASSERT, responseType)
                .build();
    }
}
