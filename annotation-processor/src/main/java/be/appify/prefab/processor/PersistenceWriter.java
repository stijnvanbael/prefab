package be.appify.prefab.processor;

import be.appify.prefab.core.annotations.OutputTarget;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterSpec;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;
import java.util.stream.Stream;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.Modifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

import static org.apache.commons.text.WordUtils.capitalize;

class PersistenceWriter {
    private final FileOutput fileWriter;
    private final PrefabContext context;

    PersistenceWriter(PrefabContext context) {
        this.context = context;
        fileWriter = new OutputTargetFileOutput(context, "application", OutputTarget.MAIN);
    }

    void writePersistenceLayer(ClassManifest manifest) {
        fileWriter.setOriginatingElements(manifest.type().asElement());
        writeRepository(manifest);
    }

    void writePolymorphicPersistenceLayer(PolymorphicAggregateManifest manifest) {
        writePolymorphicRepository(manifest);
    }

    private ParameterizedTypeName pageOf(TypeName typeName) {
        return ParameterizedTypeName.get(ClassName.get(Page.class), typeName);
    }

    private void writeRepository(ClassManifest manifest) {
        var repositoryName = "%sRepository".formatted(manifest.simpleName());
        var mixins = context.incrementalState().mixins().forAggregate(manifest.type());
        fileWriter.setOriginatingElements(Stream.concat(
                        Stream.of(manifest.type().asElement()),
                        mixins.stream())
                .distinct()
                .toArray(TypeElement[]::new));
        var type = TypeSpec.interfaceBuilder(repositoryName)
                .addModifiers(Modifier.PUBLIC)
                .addSuperinterface(ParameterizedTypeName.get(ClassName.get(CrudRepository.class),
                        manifest.type().asTypeName(), ClassName.get(String.class)))
                .addSuperinterface(ParameterizedTypeName.get(ClassName.get(PagingAndSortingRepository.class),
                        manifest.type().asTypeName(), ClassName.get(String.class)));
        mixins.forEach(mixinType -> type.addSuperinterface(mixinType.asType()));
        findByParentMethod(manifest, type);
        new PluginContributions(context)
                .apply(manifest.type().asElement(), type, OutputTarget.TEST, plugin -> plugin.writeRepository(manifest, type));
        fileWriter.writeFile(manifest.packageName(), repositoryName, type.build());
    }

    private void writePolymorphicRepository(PolymorphicAggregateManifest manifest) {
        var repositoryName = "%sRepository".formatted(manifest.simpleName());
        var mixins = context.incrementalState().mixins().forAggregate(manifest.type());
        fileWriter.setOriginatingElements(Stream.concat(
                        Stream.of(manifest.type().asElement()),
                        mixins.stream())
                .distinct()
                .toArray(TypeElement[]::new));
        var type = TypeSpec.interfaceBuilder(repositoryName)
                .addModifiers(Modifier.PUBLIC)
                .addSuperinterface(ParameterizedTypeName.get(ClassName.get(CrudRepository.class),
                        manifest.type().asTypeName(), ClassName.get(String.class)))
                .addSuperinterface(ParameterizedTypeName.get(ClassName.get(PagingAndSortingRepository.class),
                        manifest.type().asTypeName(), ClassName.get(String.class)));
        mixins.forEach(mixinType -> type.addSuperinterface(mixinType.asType()));
        findByParentMethodForPolymorphic(manifest, type);
        fileWriter.writeFile(manifest.packageName(), repositoryName, type.build());
    }

    private void findByParentMethodForPolymorphic(PolymorphicAggregateManifest manifest, TypeSpec.Builder type) {
        manifest.parent().ifPresent(
                parent -> type.addMethod(MethodSpec.methodBuilder("findBy%s".formatted(capitalize(parent.name())))
                        .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                        .addParameter(ParameterSpec.builder(String.class, parent.name()).build())
                        .addParameter(ParameterSpec.builder(Pageable.class, "pageable").build())
                        .returns(pageOf(manifest.type().asTypeName()))
                        .build()));
    }

    private void findByParentMethod(ClassManifest manifest, TypeSpec.Builder type) {
        manifest.parent().ifPresent(
                parent -> type.addMethod(MethodSpec.methodBuilder("findBy%s".formatted(capitalize(parent.name())))
                        .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                        .addParameter(ParameterSpec.builder(String.class, parent.name()).build())
                        .addParameter(ParameterSpec.builder(Pageable.class, "pageable").build())
                        .returns(pageOf(manifest.type().asTypeName()))
                        .build()));
    }

}
