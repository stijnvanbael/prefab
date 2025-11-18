package be.appify.prefab.processor.getbyid;

import be.appify.prefab.core.annotations.rest.GetById;
import be.appify.prefab.processor.ClassManifest;
import be.appify.prefab.processor.ControllerUtil;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.MethodSpec;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import javax.lang.model.element.Modifier;

public class GetByIdTestFixtureWriter {
    public MethodSpec getByIdMethod(ClassManifest manifest) {
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
                                var json = mockMvc.perform($T.$N($S, $L)
                                                .accept($T.APPLICATION_JSON))
                                        .andExpect($T.status().isOk())
                                        .andReturn()
                                        .getResponse()
                                        .getContentAsString()""",
                        MockMvcRequestBuilders.class,
                        getById.method().toLowerCase(),
                        "/" + ControllerUtil.pathOf(manifest) + getById.path(),
                        manifest.parent().map(parent -> parent.name() + ", ").orElse("") + "id",
                        MediaType.class,
                        MockMvcResultMatchers.class)
                .addStatement("return objectMapper.readValue(json, $T.class)", returnType)
                .build();
    }
}
