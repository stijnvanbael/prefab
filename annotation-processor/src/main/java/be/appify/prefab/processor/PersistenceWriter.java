package be.appify.prefab.processor;

import be.appify.prefab.core.annotations.RepositoryMixin;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterSpec;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

import javax.lang.model.element.Modifier;
import javax.lang.model.type.MirroredTypeException;
import java.util.function.Supplier;

import static org.apache.commons.text.WordUtils.capitalize;

class PersistenceWriter {
    private final JavaFileWriter fileWriter;
    private final PrefabContext context;

    PersistenceWriter(PrefabContext context) {
        this.context = context;
        fileWriter = new JavaFileWriter(context.processingEnvironment(), "application");
    }

    void writePersistenceLayer(ClassManifest manifest) {
        writeRepository(manifest);
    }

    private ParameterizedTypeName pageOf(TypeName typeName) {
        return ParameterizedTypeName.get(ClassName.get(Page.class), typeName);
    }

    private void writeRepository(ClassManifest manifest) {
        var repositoryName = "%sRepository".formatted(manifest.simpleName());
        var mixins = context.roundEnvironment().getElementsAnnotatedWith(RepositoryMixin.class)
                .stream()
                .filter(element -> new TypeManifest(element.asType(), context.processingEnvironment())
                        .annotationsOfType(RepositoryMixin.class).stream().anyMatch(annotation ->
                                manifest.type().equals(getMirroredType(annotation::value))));
        var type = TypeSpec.interfaceBuilder(repositoryName)
                .addModifiers(Modifier.PUBLIC)
                .addSuperinterface(ParameterizedTypeName.get(ClassName.get(CrudRepository.class),
                        manifest.type().asTypeName(), ClassName.get(String.class)))
                .addSuperinterface(ParameterizedTypeName.get(ClassName.get(PagingAndSortingRepository.class),
                        manifest.type().asTypeName(), ClassName.get(String.class)));
        mixins.forEach(mixinType -> type.addSuperinterface(mixinType.asType()));
        context.plugins().forEach(plugin -> plugin.writeCrudRepository(manifest, type));
        findByParentMethod(manifest, type);
        fileWriter.writeFile(manifest.packageName(), repositoryName, type.build());
    }

    private TypeManifest getMirroredType(Supplier<Class<?>> getter) {
        try {
            return TypeManifest.of(getter.get(), context.processingEnvironment());
        } catch (MirroredTypeException e) {
            return new TypeManifest(e.getTypeMirror(), context.processingEnvironment());
        }
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
