package be.appify.prefab.processor;

import be.appify.prefab.core.service.AggregateEnvelope;
import be.appify.prefab.core.service.Reference;
import be.appify.prefab.core.util.Objects;
import be.appify.prefab.processor.spring.RepositorySupport;
import be.appify.prefab.processor.spring.SpringDataReferenceProvider;
import com.palantir.javapoet.AnnotationSpec;
import com.palantir.javapoet.ArrayTypeName;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterSpec;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jdbc.core.mapping.AggregateReference;
import org.springframework.data.relational.core.mapping.Embedded;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Component;

import javax.lang.model.element.Modifier;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static be.appify.prefab.processor.CaseUtil.toSnakeCase;
import static java.util.Collections.emptyList;
import static org.apache.commons.text.WordUtils.capitalize;

public class PersistenceWriter {
    private final JavaFileWriter fileWriter;
    private final PrefabContext context;

    public PersistenceWriter(PrefabContext context) {
        this.context = context;
        fileWriter = new JavaFileWriter(context.processingEnvironment(), "infrastructure.persistence");
    }

    public void writePersistenceLayer(ClassManifest manifest) {
        writeRepositoryAdapter(manifest);
        writeDataRecord(manifest);
        writeCrudRepository(manifest);
    }

    private void writeRepositoryAdapter(ClassManifest manifest) {
        var type = TypeSpec.classBuilder("%sRepositoryAdapter".formatted(manifest.simpleName()))
                .addModifiers(Modifier.PUBLIC)
                .addSuperinterface(ClassName.get("%s.application".formatted(manifest.packageName()),
                        "%sRepository".formatted(manifest.simpleName())))
                .addAnnotation(Component.class)
                .addField(crudRepository(manifest), "repository", Modifier.PRIVATE, Modifier.FINAL)
                .addField(ClassName.get(SpringDataReferenceProvider.class), "referenceProvider", Modifier.PRIVATE,
                        Modifier.FINAL)
                .addMethod(MethodSpec.constructorBuilder()
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(ParameterSpec.builder(crudRepository(manifest), "repository").build())
                        .addParameter(
                                ParameterSpec.builder(ClassName.get(SpringDataReferenceProvider.class), "referenceProvider")
                                        .build())
                        .addStatement("this.repository = repository")
                        .addStatement("this.referenceProvider = referenceProvider")
                        .addStatement("referenceProvider.register(this)")
                        .build())
                .addMethod(MethodSpec.methodBuilder("save")
                        .addModifiers(Modifier.PUBLIC)
                        .addAnnotation(Override.class)
                        .addParameter(ParameterSpec.builder(aggregatedEnvelopeOf(manifest.className()), "envelope").build())
                        .returns(aggregatedEnvelopeOf(manifest.className()))
                        .addStatement("$T.handleErrors(() -> repository.save($T.from(envelope)))", ClassName.get(RepositorySupport.class), dataType(manifest.type()))
                        .addStatement("return envelope")
                        .build())
                .addMethod(MethodSpec.methodBuilder("getById")
                        .addModifiers(Modifier.PUBLIC)
                        .addAnnotation(Override.class)
                        .addParameter(String.class, "id")
                        .returns(optionalOf(aggregatedEnvelopeOf(manifest.className())))
                        .addStatement("return $T.handleErrors(() -> repository.findById(id).map(data -> data.toAggregate(referenceProvider)))", ClassName.get(RepositorySupport.class))
                        .build())
                .addMethod(MethodSpec.methodBuilder("findAll")
                        .addModifiers(Modifier.PUBLIC)
                        .addAnnotation(Override.class)
                        .returns(
                                ParameterizedTypeName.get(ClassName.get(Stream.class), aggregatedEnvelopeOf(manifest.className())))
                        .addStatement(
                                "return $T.handleErrors(() -> $T.stream(repository.findAll().spliterator(), false)\n" +
                                        ".map(data -> data.toAggregate(referenceProvider)))",
                                ClassName.get(RepositorySupport.class),
                                ClassName.get(StreamSupport.class))
                        .build())
                .addMethod(MethodSpec.methodBuilder("exists")
                        .addModifiers(Modifier.PUBLIC)
                        .addAnnotation(Override.class)
                        .addParameter(String.class, "id")
                        .returns(boolean.class)
                        .addStatement("return $T.handleErrors(() -> repository.existsById(id))", ClassName.get(RepositorySupport.class))
                        .build());
        context.plugins().forEach(plugin -> plugin.writeRepositoryAdapter(manifest, type));
        fileWriter.writeFile(manifest.packageName(), "%sRepositoryAdapter".formatted(manifest.simpleName()),
                type.build());
    }

    private static ClassName crudRepository(ClassManifest manifest) {
        return ClassName.get("%s.infrastructure.persistence".formatted(manifest.packageName()),
                "%sCrudRepository".formatted(manifest.simpleName()));
    }

    private ParameterizedTypeName pageOf(TypeName typeName) {
        return ParameterizedTypeName.get(ClassName.get(Page.class), typeName);
    }

    private ParameterizedTypeName optionalOf(TypeName typeName) {
        return ParameterizedTypeName.get(ClassName.get(Optional.class), typeName);
    }

    private ParameterizedTypeName aggregatedEnvelopeOf(TypeName typeName) {
        return ParameterizedTypeName.get(ClassName.get(AggregateEnvelope.class), typeName);
    }

    private void writeDataRecord(ClassManifest manifest) {
        manifest.fields().stream()
                .filter(field -> field.type().is(List.class) && field.type().parameters().getFirst().isRecord())
                .forEach(field -> writeDataRecord(field.type().parameters().getFirst().asClassManifest()));
        manifest.fields().stream()
                .filter(field -> field.type().isRecord())
                .forEach(field -> writeDataRecord(field.type().asClassManifest()));
        var type = TypeSpec.recordBuilder("%sData".formatted(manifest.simpleName()))
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(AnnotationSpec.builder(Table.class)
                        .addMember("value", "$S", toSnakeCase(manifest.simpleName()))
                        .build())
                .recordConstructor(recordConstructor(manifest));
        if (manifest.isAggregate()) {
            type.addMethod(fromAggregate(manifest))
                    .addMethod(toAggregate(manifest));
        } else {
            type.addMethod(fromEntity(manifest))
                    .addMethod(toEntity(manifest));
        }
        fileWriter.writeFile(manifest.packageName(), "%sData".formatted(manifest.simpleName()), type.build());
    }

    private MethodSpec toAggregate(ClassManifest manifest) {
        return MethodSpec.methodBuilder("toAggregate")
                .addModifiers(Modifier.PUBLIC)
                .addParameter(SpringDataReferenceProvider.class, "reference")
                .returns(aggregatedEnvelopeOf(manifest.className()))
                .addStatement("return new $T($L, id, version)",
                        AggregateEnvelope.class,
                        CodeBlock.of("new $T($L)", manifest.type().asTypeName(), manifest.fields().stream()
                                .map(this::dataField)
                                .collect(CodeBlock.joining(",\n"))))
                .build();
    }

    private MethodSpec fromAggregate(ClassManifest manifest) {
        return MethodSpec.methodBuilder("from")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(aggregatedEnvelopeOf(manifest.className()), "envelope")
                .returns(dataType(manifest.type()))
                .addStatement("return new $T(envelope.id(), envelope.version(), $L)",
                        dataType(manifest.type()), manifest.fields().stream()
                                .map(field -> entityField("envelope.aggregate().%s()".formatted(field.name()), field.type()))
                                .collect(CodeBlock.joining(",\n")))
                .build();
    }

    private CodeBlock entityField(String name, TypeManifest type) {
        if (type.is(Reference.class)) {
            return CodeBlock.of(
                    "$T.mapIfNotNull($L, reference -> $T.<$T, $T>to(reference.id()))",
                    Objects.class,
                    name,
                    AggregateReference.class,
                    dataType(type.parameters().getFirst()),
                    String.class);
        } else if (type.is(List.class)) {
            return CodeBlock.of("$L.stream().map(item -> $L).toList()", name,
                    entityField("item", type.parameters().getFirst()));
        } else if (type.isRecord()) {
            return CodeBlock.of("$T.from($L)", dataType(type), name);
        } else if (type.is(File.class)) {
            return CodeBlock.of("$T.getBytes($L)", ClassName.get(BinaryUtil.class), name);
        }
        return CodeBlock.of(name);
    }

    private MethodSpec toEntity(ClassManifest manifest) {
        return MethodSpec.methodBuilder("toEntity")
                .addModifiers(Modifier.PUBLIC)
                .addParameter(SpringDataReferenceProvider.class, "reference")
                .returns(manifest.className())
                .addStatement("return new $T($L)", manifest.type().asTypeName(), manifest.fields().stream()
                        .map(this::dataField).collect(CodeBlock.joining(",\n")))
                .build();
    }

    private CodeBlock dataField(VariableManifest field) {
        if (field.type().is(Reference.class)) {
            return CodeBlock.of("reference.referenceTo($N, $T.class)",
                    field.name(), field.type().parameters().getFirst().asTypeName());
        } else if (field.type().is(List.class)) {
            return CodeBlock.of("$L.stream().map(item -> $L).collect($T.toCollection($T::new))",
                    field.name(),
                    dataField(new VariableManifest(field.type().parameters().getFirst(), "item", emptyList(),
                            context.processingEnvironment())),
                    Collectors.class, ArrayList.class);
        } else if (field.type().isRecord()) {
            return CodeBlock.of("$L.toEntity(reference)", field.name());
        } else if (field.type().is(File.class)) {
            return CodeBlock.of("$T.toFile($L)", ClassName.get(BinaryUtil.class), field.name());
        }
        return CodeBlock.of(field.name());
    }

    private MethodSpec fromEntity(ClassManifest manifest) {
        return MethodSpec.methodBuilder("from")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(manifest.className(), "entity")
                .returns(dataType(manifest.type()))
                .addStatement("return new $T($L)",
                        dataType(manifest.type()), manifest.fields().stream()
                                .map(field -> entityField("entity.%s()".formatted(field.name()), field.type()))
                                .collect(CodeBlock.joining(",\n")))
                .build();
    }

    private MethodSpec recordConstructor(ClassManifest manifest) {
        var constructor = MethodSpec.compactConstructorBuilder()
                .addModifiers(Modifier.PUBLIC);
        if (manifest.isAggregate()) {
            constructor
                    .addParameter(ParameterSpec.builder(String.class, "id")
                            .addAnnotation(Id.class)
                            .build())
                    .addParameter(ParameterSpec.builder(int.class, "version")
                            .addAnnotation(Version.class)
                            .build());
        }
        return constructor.addParameters(manifest.fields().stream()
                        .map(VariableManifest::toBoxed)
                        .map(param -> {
                            var builder = ParameterSpec.builder(parameterType(param.type()), param.name());
                            if (!param.type().packageName().startsWith("java")
                                    && !param.type().is(Reference.class)
                                    && !param.type().isEnum()) {
                                builder.addAnnotation(AnnotationSpec.builder(Embedded.Nullable.class)
                                        .addMember("prefix", "$S", toSnakeCase(param.name()) + "_")
                                        .build());
                            }
                            return builder.build();
                        })
                        .toList())
                .build();
    }

    private TypeName parameterType(TypeManifest type) {
        if (type.is(Reference.class)) {
            return aggregateReferenceOf(type.parameters().getFirst()
                    .asTypeName("%s.infrastructure.persistence", "%sData"));
        } else if (type.is(List.class)) {
            return ParameterizedTypeName.get(ClassName.get(List.class), parameterType(type.parameters().getFirst()));
        } else if (type.isRecord()) {
            return dataType(type);
        } else if (type.is(File.class)) {
            return ArrayTypeName.of(byte.class);
        }
        return type.asTypeName();
    }

    private TypeName aggregateReferenceOf(TypeName typeName) {
        return ParameterizedTypeName.get(ClassName.get(AggregateReference.class), typeName,
                ClassName.get(String.class));
    }

    private void writeCrudRepository(ClassManifest manifest) {
        var type = TypeSpec.interfaceBuilder("%sCrudRepository".formatted(manifest.simpleName()))
                .addModifiers(Modifier.PUBLIC)
                .addSuperinterface(ParameterizedTypeName.get(ClassName.get(CrudRepository.class),
                        dataType(manifest.type()), ClassName.get(String.class)))
                .addSuperinterface(ParameterizedTypeName.get(ClassName.get(PagingAndSortingRepository.class),
                        dataType(manifest.type()), ClassName.get(String.class)));
        context.plugins().forEach(plugin -> plugin.writeCrudRepository(manifest, type));
        findByParentMethod(manifest, type);
        fileWriter.writeFile(manifest.packageName(), "%sCrudRepository".formatted(manifest.simpleName()), type.build());
    }

    private void findByParentMethod(ClassManifest manifest, TypeSpec.Builder type) {
        manifest.parent().ifPresent(parent -> type.addMethod(MethodSpec.methodBuilder("findBy%s".formatted(capitalize(parent.name())))
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addParameter(ParameterSpec.builder(String.class, parent.name()).build())
                .addParameter(ParameterSpec.builder(Pageable.class, "pageable").build())
                .returns(pageOf(dataType(manifest.type())))
                .build()));
    }

    private ClassName dataType(TypeManifest manifest) {
        return ClassName.get("%s.infrastructure.persistence".formatted(manifest.packageName()),
                "%sData".formatted(manifest.simpleName()));
    }
}
