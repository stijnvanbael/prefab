package be.appify.prefab.processor.rest.binary;

import be.appify.prefab.processor.ClassManifest;
import be.appify.prefab.processor.VariableManifest;
import be.appify.prefab.processor.rest.ControllerUtil;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.MethodSpec;
import javax.lang.model.element.Modifier;

import static be.appify.prefab.processor.TestClasses.MOCK_MVC_RESULT_MATCHERS;
import static org.apache.commons.lang3.StringUtils.capitalize;

class BinaryTestClientWriter {
    MethodSpec downloadMethod(ClassManifest manifest, VariableManifest field) {
        MethodSpec.Builder method = MethodSpec.methodBuilder("download%s".formatted(capitalize(field.name())))
                .addModifiers(Modifier.PUBLIC)
                .returns(byte[].class)
                .addException(Exception.class)
                .addParameter(String.class, "id");
        manifest.parent().ifPresent(parent -> method.addParameter(String.class, parent.name()));
        return method
                .addStatement("""
                                return mockMvc.perform($T.get($S, $L))
                                .andExpect($T.status().isOk())
                                .andReturn()
                                .getResponse()
                                .getContentAsByteArray()""",
                        ClassName.get("org.springframework.test.web.servlet.request", "MockMvcRequestBuilders"),
                        "/" + ControllerUtil.pathOf(manifest) + "/{id}/" + field.name(),
                        manifest.parent().map(parent -> parent.name() + ", ").orElse("") + "id",
                        MOCK_MVC_RESULT_MATCHERS)
                .build();
    }
}
