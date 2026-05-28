package be.appify.prefab.processor;

import be.appify.prefab.core.annotations.OutputTarget;
import com.palantir.javapoet.TypeSpec;
import java.util.EnumMap;
import java.util.Map;
import javax.lang.model.element.TypeElement;

/**
 * Delegates file writes to main or test output based on the effective output target.
 */
public final class OutputTargetFileOutput implements TestFileOutput {
    private final PrefabContext context;
    private final OutputTarget defaultTarget;
    private final TestJavaFileWriter testWriter;
    private final Map<OutputTarget, FileOutput> writersByTarget;

    public OutputTargetFileOutput(PrefabContext context, String packageSuffix, OutputTarget defaultTarget) {
        this.context = context;
        this.defaultTarget = defaultTarget;
        var mainWriter = new JavaFileWriter(context.processingEnvironment(), packageSuffix);
        this.testWriter = new TestJavaFileWriter(context, packageSuffix);
        this.writersByTarget = new EnumMap<>(OutputTarget.class);
        writersByTarget.put(OutputTarget.MAIN, mainWriter);
        writersByTarget.put(OutputTarget.TEST, testWriter);
    }

    @Override
    public void setPreferredElement(TypeElement element) {
        testWriter.setPreferredElement(element);
    }

    @Override
    public void writeFile(String packagePrefix, String typeName, TypeSpec type) {
        var target = PluginOutputScope.effectiveTargetFor(context, defaultTarget);
        writersByTarget.getOrDefault(target, writersByTarget.get(OutputTarget.MAIN))
                .writeFile(packagePrefix, typeName, type);
    }
}


