package be.appify.prefab.processor;

import com.palantir.javapoet.JavaFile;
import com.palantir.javapoet.TypeSpec;
import javax.lang.model.element.Modifier;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class TestClientWriterTest {

    @Test
    void constructorAcceptsMockMvcConfigurerList() {
        var constructor = TestClientWriter.buildConstructor();

        var source = toSource(constructor);

        assertTrue(source.contains("List<MockMvcConfigurer> configurers"), source);
    }

    @Test
    void constructorSortsConfigurersByOrder() {
        var constructor = TestClientWriter.buildConstructor();

        var source = toSource(constructor);

        assertTrue(source.contains("AnnotationAwareOrderComparator.sort(configurers)"), source);
    }

    @Test
    void constructorAppliesConfigurersToBuildingMockMvc() {
        var constructor = TestClientWriter.buildConstructor();

        var source = toSource(constructor);

        assertTrue(source.contains("configurers.forEach(builder::apply)"), source);
    }

    private static String toSource(com.palantir.javapoet.MethodSpec constructor) {
        var type = TypeSpec.classBuilder("TestClient")
                .addModifiers(Modifier.PUBLIC)
                .addMethod(constructor)
                .build();
        return JavaFile.builder("test", type).build().toString();
    }
}


