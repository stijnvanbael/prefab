package be.appify.prefab.processor.error;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import be.appify.prefab.processor.JavaFileWriter;
import com.palantir.javapoet.TypeSpec;
import java.io.IOException;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Modifier;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class GenerationIoFailureTest {

    @SuppressWarnings("unchecked")
    private static <T> T fake(Class<T> type, java.lang.reflect.InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, handler);
    }

    private static ProcessingEnvironment environment(Filer filer, Messager messager) {
        return fake(ProcessingEnvironment.class, (proxy, method, args) -> switch (method.getName()) {
            case "getFiler" -> filer;
            case "getMessager" -> messager;
            case "getOptions" -> Map.of();
            case "hashCode" -> System.identityHashCode(proxy);
            case "equals" -> proxy == args[0];
            default -> throw new UnsupportedOperationException(method.getName());
        });
    }

    @Test
    @DisplayName("an I/O failure while writing generated source is an expected failure, not a raw RuntimeException")
    void writeFile_ioFailure_throwsActionableProcessingException() {
        var file = fake(JavaFileObject.class, (proxy, method, args) -> {
            if (method.getName().equals("openWriter")) {
                throw new IOException("No space left on device");
            }
            throw new UnsupportedOperationException(method.getName());
        });
        var filer = fake(Filer.class, (proxy, method, args) -> file);
        var writer = new JavaFileWriter(environment(filer, null), null);
        var type = TypeSpec.classBuilder("Foo").addModifiers(Modifier.PUBLIC).build();

        var failure = assertThrows(PrefabProcessingException.class, () -> writer.writeFile("test.io", "Foo", type));

        assertTrue(failure.ruleViolated().contains("test.io.Foo"));
        assertTrue(failure.ruleViolated().contains("No space left on device"));
        assertTrue(failure.suggestedFix().contains("writable"));
        assertInstanceOf(IOException.class, failure.getCause());
    }

    @Test
    @DisplayName("the guard reports the failure once, with the cause, and aborts nested boundaries silently")
    void guard_reportsOnce_andNestedBoundaryStaysSilent() {
        var messages = new ArrayList<String>();
        var kinds = new ArrayList<Diagnostic.Kind>();
        var messager = fake(Messager.class, (proxy, method, args) -> {
            kinds.add((Diagnostic.Kind) args[0]);
            messages.add(args[1].toString());
            return null;
        });
        var guard = new ProcessingGuard(new ErrorReporter(environment(null, messager)));

        var succeeded = guard.run("outer", null, () -> guard.runOrAbort("inner", null, () -> {
            throw new PrefabProcessingException(null, "Prefab", "cannot write file", "free disk space",
                    new IOException("disk full"));
        }));

        assertFalse(succeeded);
        assertEquals(List.of(Diagnostic.Kind.ERROR), kinds);
        var message = messages.getFirst();
        assertTrue(message.contains("cannot write file"));
        assertTrue(message.contains("Suggested fix: free disk space"));
        assertTrue(message.contains("java.io.IOException: disk full"));
    }
}
