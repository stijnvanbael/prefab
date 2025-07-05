package be.appify.prefab.processor;

import be.appify.prefab.core.repository.Repository;
import be.appify.prefab.processor.spring.SpringService;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.FieldSpec;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterSpec;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.lang.model.element.Modifier;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.apache.commons.text.WordUtils.uncapitalize;

public class ApplicationWriter {
    private final JavaFileWriter fileWriter;
    private final PrefabContext context;

    public ApplicationWriter(PrefabContext context) {
        this.context = context;
        fileWriter = new JavaFileWriter(context.processingEnvironment(), "application");
    }

    public void writeApplicationLayer(ClassManifest manifest) {
        writeRepositoryInterface(manifest);
        writeService(manifest);
    }

    private void writeService(ClassManifest manifest) {
        var serviceName = "%sService".formatted(manifest.simpleName());
        var type = TypeSpec.classBuilder(serviceName)
                .addModifiers(Modifier.PUBLIC)
                .addSuperinterface(ClassName.get(SpringService.class))
                .addAnnotation(ClassName.get(Component.class))
                .addAnnotation(ClassName.get(Transactional.class))
                .addField(FieldSpec.builder(Logger.class, "log", Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                        .initializer("$T.getLogger($T.class)", ClassName.get(LoggerFactory.class), ClassName.get(manifest.packageName() + ".application", serviceName))
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
        dependencies.forEach(dependency -> constructor.addStatement("this.$N = $N", nameOf(dependency), nameOf(dependency)));
        type.addMethod(constructor.build());
        context.plugins().forEach(plugin -> plugin.writeService(manifest, type, context));
        fileWriter.writeFile(manifest.packageName(), serviceName, type.build());
    }

    private String nameOf(TypeName type) {
        return type instanceof ClassName className
                ? uncapitalize(className.simpleName())
                : uncapitalize(type.toString().substring(type.toString().lastIndexOf('.') + 1));
    }

    private Set<TypeName> collectDependencies(ClassManifest manifest) {
        return Stream.concat(context.plugins().stream().flatMap(plugin ->
                                plugin.getServiceDependencies(manifest, context).stream()),
                        Stream.of(ClassName.get("%s.application".formatted(manifest.type().packageName()),
                                "%sRepository".formatted(manifest.type().simpleName()))))
                .collect(Collectors.toSet());
    }

    private void writeRepositoryInterface(ClassManifest manifest) {
        var aggregateType = manifest.type().asTypeName();
        var type = TypeSpec.interfaceBuilder("%sRepository".formatted(manifest.simpleName()))
                .addModifiers(Modifier.PUBLIC)
                .addSuperinterface(
                        ParameterizedTypeName.get(ClassName.get(Repository.class), aggregateType))
                .addMethod(MethodSpec.methodBuilder("aggregateType")
                        .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
                        .returns(ParameterizedTypeName.get(ClassName.get(Class.class), aggregateType))
                        .addStatement("return $T.class", aggregateType)
                        .build());
        context.plugins().forEach(plugin -> plugin.writeRepository(manifest, type));
        fileWriter.writeFile(manifest.packageName(), "%sRepository".formatted(manifest.simpleName()), type.build());
    }
}
