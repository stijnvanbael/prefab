package be.appify.prefab.processor.delete;

import be.appify.prefab.core.annotations.rest.Delete;
import be.appify.prefab.processor.ClassManifest;
import be.appify.prefab.processor.ControllerUtil;
import com.palantir.javapoet.MethodSpec;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import javax.lang.model.element.Modifier;
import java.util.List;

public class DeleteTestFixtureWriter {
    public List<MethodSpec> deleteMethods(ClassManifest manifest) {
        return List.of(deleteMethod(manifest), whenVariant(manifest), givenVariant(manifest));
    }

    private MethodSpec whenVariant(ClassManifest manifest) {
        return variant(manifest, "whenDeleting" + manifest.simpleName());
    }

    private MethodSpec givenVariant(ClassManifest manifest) {
        return variant(manifest, "given" + manifest.simpleName() + "Deleted");
    }

    private static MethodSpec variant(ClassManifest manifest, String methodName) {
        return MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.PUBLIC)
                .returns(void.class)
                .addParameter(String.class, "id")
                .addException(Exception.class)
                .addStatement("delete$L(id)", manifest.simpleName())
                .build();
    }

    private static MethodSpec deleteMethod(ClassManifest manifest) {
        var delete = manifest.annotationsOfType(Delete.class).stream().findFirst().orElseThrow();
        return MethodSpec.methodBuilder("delete" + manifest.simpleName())
                .addModifiers(Modifier.PUBLIC)
                .returns(void.class)
                .addParameter(String.class, "id")
                .addException(Exception.class)
                .addStatement("""
                                mockMvc.perform($T.$N($S, id))
                                        .andExpect($T.status().isNoContent())""",
                        MockMvcRequestBuilders.class,
                        delete.method().toLowerCase(),
                        "/" + ControllerUtil.pathOf(manifest) + delete.path(),
                        MockMvcResultMatchers.class)
                .build();
    }
}
