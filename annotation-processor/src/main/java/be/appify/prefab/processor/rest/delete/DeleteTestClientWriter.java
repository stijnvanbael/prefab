package be.appify.prefab.processor.rest.delete;

import be.appify.prefab.core.annotations.rest.Delete;
import be.appify.prefab.processor.ClassManifest;
import be.appify.prefab.processor.rest.ControllerUtil;
import com.palantir.javapoet.MethodSpec;
import java.util.List;
import javax.lang.model.element.Modifier;

import static be.appify.prefab.processor.TestClasses.MOCK_MVC_REQUEST_BUILDERS;
import static be.appify.prefab.processor.TestClasses.MOCK_MVC_RESULT_MATCHERS;

class DeleteTestClientWriter {
    List<MethodSpec> deleteMethods(ClassManifest manifest) {
        return List.of(deleteMethod(manifest), whenVariant(manifest), givenVariant(manifest));
    }

    private MethodSpec whenVariant(ClassManifest manifest) {
        return variant(manifest, "whenDeleting" + manifest.simpleName());
    }

    private MethodSpec givenVariant(ClassManifest manifest) {
        return variant(manifest, "given" + manifest.simpleName() + "Deleted");
    }

    private static MethodSpec variant(ClassManifest manifest, String methodName) {
        var method = MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.PUBLIC)
                .returns(void.class)
                .addParameter(String.class, "id");
        manifest.parent().ifPresent(parent -> method.addParameter(String.class, parent.name()));
        return method
                .addException(Exception.class)
                .addStatement("delete$L($L)",
                        manifest.simpleName(),
                        "id" + manifest.parent().map(parent -> ", " + parent.name()).orElse(""))
                .build();
    }

    private static MethodSpec deleteMethod(ClassManifest manifest) {
        var delete = manifest.annotationsOfType(Delete.class).stream().findFirst()
                .or(() -> manifest.methodsWith(Delete.class).stream().findFirst()
                        .map(method -> method.getAnnotation(Delete.class)))
                .orElseThrow();
        var method = MethodSpec.methodBuilder("delete" + manifest.simpleName())
                .addModifiers(Modifier.PUBLIC)
                .returns(void.class)
                .addParameter(String.class, "id");
        manifest.parent().ifPresent(parent -> method.addParameter(String.class, parent.name()));
        return method
                .addException(Exception.class)
                .addStatement("""
                                mockMvc.perform($T.$N($S, $L)$L)
                                        .andExpect($T.status().isNoContent())""",
                        MOCK_MVC_REQUEST_BUILDERS,
                        delete.method().toLowerCase(),
                        "/" + ControllerUtil.pathOf(manifest) + delete.path(),
                        manifest.parent().map(parent -> parent.name() + ", ").orElse("") + "id",
                        ControllerUtil.withMockUser(delete.security()),
                        MOCK_MVC_RESULT_MATCHERS)
                .build();
    }
}
