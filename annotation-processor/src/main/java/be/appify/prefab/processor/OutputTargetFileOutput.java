package be.appify.prefab.processor;

import be.appify.prefab.core.annotations.OutputTarget;
import com.palantir.javapoet.TypeSpec;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import javax.lang.model.element.TypeElement;

/**
 * Delegates file writes to main or test output based on the effective output target.
 */
public final class OutputTargetFileOutput implements FileOutput {
    private final PrefabContext context;
    private final OutputTarget defaultTarget;
    private final TestJavaFileWriter testWriter;
    private final String packageSuffix;
    private final Map<OutputTarget, FileOutput> writersByTarget;

    public OutputTargetFileOutput(PrefabContext context, String packageSuffix, OutputTarget defaultTarget) {
        this.context = context;
        this.defaultTarget = defaultTarget;
        this.packageSuffix = packageSuffix;
        var mainWriter = new JavaFileWriter(context.processingEnvironment(), packageSuffix);
        this.testWriter = new TestJavaFileWriter(context, packageSuffix);
        this.writersByTarget = new EnumMap<>(OutputTarget.class);
        writersByTarget.put(OutputTarget.MAIN, mainWriter);
        writersByTarget.put(OutputTarget.TEST, testWriter);
    }

    @Override
    public void setPreferredElement(TypeElement element) {
        setOriginatingElements(element);
    }

    @Override
    public void setOriginatingElements(TypeElement... elements) {
        writersByTarget.values().forEach(writer -> writer.setOriginatingElements(elements));
    }

    @Override
    public void writeFile(String packagePrefix, String typeName, TypeSpec type) {
        writersByTarget.get(target())
                .writeFile(packagePrefix, typeName, type);
    }

    /** @return the effective destination for this writer in the current plugin scope */
    public OutputTarget target() {
        return PluginOutputScope.effectiveTargetFor(context, defaultTarget);
    }

    /** Records additional owners of a deduplicated test helper without writing the source twice. */
    public void alsoOwnedBy(String packageName, String typeName, TypeElement owner) {
        if (target() == OutputTarget.TEST) {
            var pkg = packageSuffix == null || packageSuffix.isBlank() ? packageName : packageName + "." + packageSuffix;
            var path = (pkg.isEmpty() ? "" : pkg.replace('.', '/') + "/") + typeName + ".java";
            context.incrementalState().record(path, List.of(owner), context);
        }
    }
}
