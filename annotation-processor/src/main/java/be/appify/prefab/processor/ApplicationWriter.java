package be.appify.prefab.processor;

import be.appify.prefab.core.annotations.rest.GetById;
import be.appify.prefab.core.annotations.rest.GetList;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.FieldSpec;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterSpec;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.lang.model.element.Modifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import static org.apache.commons.text.WordUtils.uncapitalize;
import static org.atteo.evo.inflector.English.plural;

class ApplicationWriter {
    private static final ClassName TRANSACTIONAL = ClassName.get("org.springframework.transaction.annotation", "Transactional");
    private final JavaFileWriter fileWriter;
    private final PrefabContext context;

    ApplicationWriter(PrefabContext context) {
        this.context = context;
        fileWriter = new JavaFileWriter(context.processingEnvironment(), "application");
    }

    void writeApplicationLayer(ClassManifest manifest) {
        writeService(manifest);
    }

    void writePolymorphicApplicationLayer(PolymorphicAggregateManifest manifest) {
        writePolymorphicService(manifest);
    }

    private void writeService(ClassManifest manifest) {
        var serviceName = "%sService".formatted(manifest.simpleName());
        var type = TypeSpec.classBuilder(serviceName)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(ClassName.get(Component.class))
                .addAnnotation(TRANSACTIONAL);
        type.addField(FieldSpec.builder(Logger.class, "log", Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
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

    private void writePolymorphicService(PolymorphicAggregateManifest manifest) {
        var serviceName = "%sService".formatted(manifest.simpleName());
        var repositoryType = ClassName.get("%s.application".formatted(manifest.packageName()),
                "%sRepository".formatted(manifest.simpleName()));
        var repositoryName = uncapitalize(manifest.simpleName()) + "Repository";
        var domainType = manifest.type().asTypeName();
        var type = TypeSpec.classBuilder(serviceName)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(ClassName.get(Component.class))
                .addAnnotation(TRANSACTIONAL);
        type.addField(FieldSpec.builder(Logger.class, "log", Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                .initializer("$T.getLogger($T.class)", ClassName.get(LoggerFactory.class),
                        ClassName.get(manifest.packageName() + ".application", serviceName))
                .build());
        type.addField(FieldSpec.builder(repositoryType, repositoryName, Modifier.PRIVATE, Modifier.FINAL).build());
        type.addMethod(MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addParameter(repositoryType, repositoryName)
                .addStatement("this.$N = $N", repositoryName, repositoryName)
                .build());

        // getById method
        if (!manifest.annotationsOfType(GetById.class).isEmpty()) {
            type.addMethod(MethodSpec.methodBuilder("getById")
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(String.class, "id")
                    .returns(ParameterizedTypeName.get(ClassName.get(Optional.class), domainType))
                    .addStatement("log.debug($S, $T.class.getSimpleName(), id)", "Getting {} by id: {}",
                            manifest.type().asTypeName())
                    .addStatement("return $N.findById(id)", repositoryName)
                    .build());
        }

        // getList method
        if (!manifest.annotationsOfType(GetList.class).isEmpty()) {
            type.addMethod(MethodSpec.methodBuilder("getList")
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(Pageable.class, "pageable")
                    .returns(ParameterizedTypeName.get(ClassName.get(Page.class), domainType))
                    .addStatement("log.debug($S)", "Getting " + plural(manifest.simpleName()))
                    .addStatement("return $N.findAll(pageable)", repositoryName)
                    .build());
        }

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
