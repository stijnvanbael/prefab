package be.appify.prefab.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.palantir.javapoet.FieldSpec;
import com.palantir.javapoet.TypeSpec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.MockMvc;

import javax.lang.model.element.Modifier;

public class TestWriter {
    private final TestJavaFileWriter fileWriter;
    private final PrefabContext context;

    public TestWriter(PrefabContext context) {
        this.context = context;
        fileWriter = new TestJavaFileWriter(context, null);
    }

    public void writeTestSupport(ClassManifest manifest) {
        writeTestFixture(manifest);
    }

    private void writeTestFixture(ClassManifest manifest) {
        var className = "%sFixture".formatted(manifest.simpleName());
        var type = TypeSpec.classBuilder(className)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Component.class)
                .addField(FieldSpec.builder(MockMvc.class, "mockMvc")
                        .addAnnotation(Autowired.class)
                        .build())
                .addField(FieldSpec.builder(ObjectMapper.class, "objectMapper")
                        .addAnnotation(Autowired.class)
                        .build());
        context.plugins().forEach(plugin -> plugin.writeTestFixture(manifest, type, context));

        fileWriter.writeFile(manifest.packageName(), className, type.build());
    }
}
