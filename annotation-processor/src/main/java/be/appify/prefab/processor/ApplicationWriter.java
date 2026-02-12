package be.appify.prefab.processor;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.FieldSpec;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterSpec;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.lang.model.element.Modifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import static org.apache.commons.text.WordUtils.uncapitalize;

class ApplicationWriter {
    private final JavaFileWriter fileWriter;
    private final PrefabContext context;

    ApplicationWriter(PrefabContext context) {
        this.context = context;
        fileWriter = new JavaFileWriter(context.processingEnvironment(), "application");
    }

    void writeApplicationLayer(ClassManifest manifest) {
        writeService(manifest);
    }

    private void writeService(ClassManifest manifest) {
        var serviceName = "%sService".formatted(manifest.simpleName());
        var type = TypeSpec.classBuilder(serviceName)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(ClassName.get(Component.class))
                .addAnnotation(ClassName.get(Transactional.class))
                .addField(FieldSpec.builder(Logger.class, "log", Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                        .initializer("$T.getLogger($T.class)", ClassName.get(LoggerFactory.class),
                                ClassName.get(manifest.packageName() + ".application", serviceName))
                        .build());
        var dependencies = collectDependencies(manifest);
        dependencies.forEach(dependency -> type.addField(FieldSpec.builder(
                        dependency,
                        nameOf(dependency))
                .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
                .build()));
        var constructor = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addParameters(dependencies.stream().map(dependency ->
                                ParameterSpec.builder(dependency, nameOf(dependency))
                                        .build())
                        .toList());
        dependencies.forEach(
                dependency -> constructor.addStatement("this.$N = $N", nameOf(dependency), nameOf(dependency)));
        type.addMethod(constructor.build());
        context.plugins().forEach(plugin -> plugin.writeService(manifest, type));
        fileWriter.writeFile(manifest.packageName(), serviceName, type.build());
    }

    private String nameOf(TypeName type) {
        return type instanceof ClassName className
                ? uncapitalize(className.simpleName())
                : uncapitalize(type.toString().substring(type.toString().lastIndexOf('.') + 1));
    }

    private Set<TypeName> collectDependencies(ClassManifest manifest) {
        return Stream.concat(context.plugins().stream().flatMap(plugin ->
                                plugin.getServiceDependencies(manifest).stream()),
                        Stream.of(ClassName.get("%s.application".formatted(manifest.type().packageName()),
                                "%sRepository".formatted(manifest.type().simpleName()))))
                .collect(Collectors.toSet());
    }
}
