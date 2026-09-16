package be.appify.prefab.processor.rest.binary;

import be.appify.prefab.core.annotations.rest.Download;
import be.appify.prefab.processor.AnnotationManifest;
import be.appify.prefab.processor.ClassManifest;
import be.appify.prefab.processor.VariableManifest;
import be.appify.prefab.processor.rest.ControllerUtil;
import com.palantir.javapoet.MethodSpec;
import javax.lang.model.element.Modifier;

import static be.appify.prefab.processor.TestClasses.MOCK_MVC_REQUEST_BUILDERS;
import static be.appify.prefab.processor.TestClasses.MOCK_MVC_RESULT_MATCHERS;
import static org.apache.commons.lang3.StringUtils.capitalize;

class BinaryTestClientWriter {
    MethodSpec downloadMethod(ClassManifest manifest, VariableManifest field) {
        var security = field.getAnnotation(Download.class).map(AnnotationManifest::value).orElseThrow().security();
        MethodSpec.Builder method = MethodSpec.methodBuilder("download%s".formatted(capitalize(field.name())))
                .addModifiers(Modifier.PUBLIC)
                .returns(byte[].class)
                .addException(Exception.class)
                .addParameter(String.class, "id");
        manifest.parent().ifPresent(parent -> method.addParameter(String.class, parent.name()));
        return method
                .addStatement("""
                                return mockMvc.perform($T.get($S, $L)$L)
                                .andExpect($T.status().isOk())
                                .andReturn()
                                .getResponse()
                                .getContentAsByteArray()""",
                        MOCK_MVC_REQUEST_BUILDERS,
                        "/" + ControllerUtil.pathOf(manifest) + "/{id}/" + field.name(),
                        manifest.parent().map(parent -> parent.name() + ", ").orElse("") + "id",
                        ControllerUtil.withMockUser(security),
                        MOCK_MVC_RESULT_MATCHERS)
                .build();
    }
}
