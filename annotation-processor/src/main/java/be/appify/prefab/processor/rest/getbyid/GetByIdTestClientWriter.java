package be.appify.prefab.processor.rest.getbyid;

import be.appify.prefab.core.annotations.rest.GetById;
import be.appify.prefab.processor.ClassManifest;
import be.appify.prefab.processor.rest.ControllerUtil;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.MethodSpec;
import javax.lang.model.element.Modifier;
import org.springframework.http.MediaType;

import static be.appify.prefab.processor.TestClasses.MOCK_MVC_REQUEST_BUILDERS;
import static be.appify.prefab.processor.TestClasses.MOCK_MVC_RESULT_MATCHERS;

class GetByIdTestClientWriter {
    MethodSpec getByIdMethod(ClassManifest manifest) {
        var getById = manifest.annotationsOfType(GetById.class).stream().findFirst().orElseThrow();
        var returnType = ClassName.get(manifest.packageName() + ".infrastructure.http",
                "%sResponse".formatted(manifest.simpleName()));
        var method = MethodSpec.methodBuilder("get" + manifest.simpleName() + "ById")
                .addModifiers(Modifier.PUBLIC)
                .returns(returnType)
                .addException(Exception.class)
                .addParameter(String.class, "id");
        manifest.parent().ifPresent(parent -> method.addParameter(String.class, parent.name()));
        return method.addStatement("""
                                var json = mockMvc.perform($T.$N($S, $L)$L
                                                .accept($T.APPLICATION_JSON))
                                        .andExpect($T.status().isOk())
                                        .andReturn()
                                        .getResponse()
                                        .getContentAsString()""",
                        MOCK_MVC_REQUEST_BUILDERS,
                        getById.method().toLowerCase(),
                        "/" + ControllerUtil.pathOf(manifest) + getById.path(),
                        manifest.parent().map(parent -> parent.name() + ", ").orElse("") + "id",
                        ControllerUtil.withMockUser(getById.security()),
                        MediaType.class,
                        MOCK_MVC_RESULT_MATCHERS)
                .addStatement("return jsonMapper.readValue(json, $T.class)", returnType)
                .build();
    }
}
