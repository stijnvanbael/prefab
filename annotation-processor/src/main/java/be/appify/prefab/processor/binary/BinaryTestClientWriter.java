package be.appify.prefab.processor.binary;

import be.appify.prefab.processor.ClassManifest;
import be.appify.prefab.processor.ControllerUtil;
import be.appify.prefab.processor.VariableManifest;
import com.palantir.javapoet.MethodSpec;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import javax.lang.model.element.Modifier;

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
                        MockMvcRequestBuilders.class,
                        "/" + ControllerUtil.pathOf(manifest) + "/{id}/" + field.name(),
                        manifest.parent().map(parent -> parent.name() + ", ").orElse("") + "id",
                        MockMvcResultMatchers.class)
                .build();
    }
}
