package be.appify.prefab.processor;

import be.appify.prefab.core.annotations.OutputTarget;
import com.palantir.javapoet.TypeSpec;
import java.util.Optional;
import javax.lang.model.element.TypeElement;

/**
 * Delegates file writes to main or test output based on the effective output target.
 */
public final class OutputTargetFileOutput implements TestFileOutput {
    private final PrefabContext context;
    private final OutputTarget defaultTarget;
    private final JavaFileWriter mainWriter;
    private final TestJavaFileWriter testWriter;

    public OutputTargetFileOutput(PrefabContext context, String packageSuffix, OutputTarget defaultTarget) {
        this.context = context;
        this.defaultTarget = defaultTarget;
        this.mainWriter = new JavaFileWriter(context.processingEnvironment(), packageSuffix);
        this.testWriter = new TestJavaFileWriter(context, packageSuffix);
    }

    @Override
    public void setPreferredElement(TypeElement element) {
        testWriter.setPreferredElement(element);
    }

    @Override
    public void writeFile(String packagePrefix, String typeName, TypeSpec type) {
        var target = effectiveTarget();
        if (target == OutputTarget.TEST) {
            testWriter.writeFile(packagePrefix, typeName, type);
            return;
        }
        mainWriter.writeFile(packagePrefix, typeName, type);
    }

    private OutputTarget effectiveTarget() {
        Optional<PluginOutputScope.State> scopedOutput = PluginOutputScope.current();
        if (scopedOutput.isPresent() && scopedOutput.get().context() == context) {
            var scopedTarget = scopedOutput.get().target();
            return scopedTarget == OutputTarget.DEFAULT ? defaultTarget : scopedTarget;
        }
        return defaultTarget;
    }
}


