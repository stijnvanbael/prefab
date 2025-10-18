package be.appify.prefab.processor;

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
import static org.apache.commons.text.WordUtils.capitalize;

import javax.lang.model.element.Modifier;

public class PersistenceWriter {
    private final JavaFileWriter fileWriter;
    private final PrefabContext context;

    public PersistenceWriter(PrefabContext context) {
        this.context = context;
        fileWriter = new JavaFileWriter(context.processingEnvironment(), "application");
    }

    public void writePersistenceLayer(ClassManifest manifest) {
        writeRepository(manifest);
    }

    private ParameterizedTypeName pageOf(TypeName typeName) {
        return ParameterizedTypeName.get(ClassName.get(Page.class), typeName);
    }

    private void writeRepository(ClassManifest manifest) {
        var repositoryName = "%sRepository".formatted(manifest.simpleName());
        var type = TypeSpec.interfaceBuilder(repositoryName)
                .addModifiers(Modifier.PUBLIC)
                .addSuperinterface(ParameterizedTypeName.get(ClassName.get(CrudRepository.class),
                        manifest.type().asTypeName(), ClassName.get(String.class)))
                .addSuperinterface(ParameterizedTypeName.get(ClassName.get(PagingAndSortingRepository.class),
                        manifest.type().asTypeName(), ClassName.get(String.class)));
        context.plugins().forEach(plugin -> plugin.writeCrudRepository(manifest, type));
        findByParentMethod(manifest, type);
        fileWriter.writeFile(manifest.packageName(), repositoryName, type.build());
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
