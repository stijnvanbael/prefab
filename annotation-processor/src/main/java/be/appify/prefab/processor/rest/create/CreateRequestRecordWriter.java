package be.appify.prefab.processor.rest.create;

import be.appify.prefab.processor.ClassManifest;
import be.appify.prefab.processor.JavaFileWriter;
import be.appify.prefab.processor.PolymorphicAggregateManifest;
import be.appify.prefab.processor.PrefabContext;
import be.appify.prefab.processor.VariableManifest;
import com.palantir.javapoet.AnnotationSpec;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.TypeSpec;
import java.util.List;
import java.util.Map;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;

import static be.appify.prefab.processor.rest.ControllerUtil.writeRecord;

class CreateRequestRecordWriter {
    void writeRequestRecord(
            JavaFileWriter fileWriter,
            ClassManifest manifest,
            ExecutableElement controller,
            PrefabContext context
    ) {
        var name = "Create%sRequest".formatted(manifest.simpleName());
        var type = writeRecord(ClassName.get(manifest.packageName() + ".application", name),
                controller.getParameters().stream()
                        .map(param ->
                                VariableManifest.of(param, context.processingEnvironment()))
                        .toList(),
                context.requestParameterBuilder());
        fileWriter.writeFile(manifest.packageName(), name, type);
    }

    void writeUnionRequestInterface(
            JavaFileWriter fileWriter,
            PolymorphicAggregateManifest polymorphic,
            List<Map.Entry<ClassManifest, ExecutableElement>> group,
            PrefabContext context
    ) {
        var unionName = "Create%sRequest".formatted(polymorphic.simpleName());
        var unionClass = ClassName.get(polymorphic.packageName() + ".application", unionName);

        var permittedSubclasses = group.stream()
                .map(e -> nestedClass(polymorphic, unionName, e.getKey()))
                .toList();

        var jsonTypeInfo = ClassName.get("com.fasterxml.jackson.annotation", "JsonTypeInfo");
        var typeInfoAnnotation = AnnotationSpec.builder(jsonTypeInfo)
                .addMember("use", "$T.Id.NAME", jsonTypeInfo)
                .addMember("property", "$S", "type")
                .build();

        var outerInterface = TypeSpec.interfaceBuilder(unionName)
                .addModifiers(Modifier.PUBLIC)
                .addModifiers(javax.lang.model.element.Modifier.SEALED)
                .addAnnotation(typeInfoAnnotation);

        permittedSubclasses.forEach(outerInterface::addPermittedSubclass);

        group.forEach(e -> outerInterface.addType(buildNestedRecord(unionClass, e, context)));

        fileWriter.writeFile(polymorphic.packageName(), unionName, outerInterface.build());
    }

    private TypeSpec buildNestedRecord(
            ClassName unionClass,
            Map.Entry<ClassManifest, ExecutableElement> entry,
            PrefabContext context
    ) {
        var leafName = leafName(entry.getKey().simpleName());
        var nestedName = "Create%sRequest".formatted(leafName);
        var params = entry.getValue().getParameters().stream()
                .map(p -> VariableManifest.of(p, context.processingEnvironment()))
                .toList();
        var recordParams = params.stream()
                .flatMap(p -> context.requestParameterBuilder().buildBodyParameter(p)
                        .or(() -> context.requestParameterBuilder().buildMethodParameter(p))
                        .stream())
                .toList();
        return TypeSpec.recordBuilder(nestedName)
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addSuperinterface(unionClass)
                .recordConstructor(MethodSpec.compactConstructorBuilder()
                        .addParameters(recordParams)
                        .build())
                .build();
    }

    private static ClassName nestedClass(PolymorphicAggregateManifest polymorphic, String unionName, ClassManifest subtype) {
        return ClassName.get(polymorphic.packageName() + ".application", unionName,
                "Create%sRequest".formatted(leafName(subtype.simpleName())));
    }

    void writeRequestRecordForPolymorphic(
            JavaFileWriter fileWriter,
            PolymorphicAggregateManifest polymorphic,
            ClassManifest subtype,
            ExecutableElement constructor,
            PrefabContext context
    ) {
        var leafName = leafName(subtype.simpleName());
        var name = "Create%sRequest".formatted(leafName);
        var type = writeRecord(ClassName.get(subtype.packageName() + ".application", name),
                constructor.getParameters().stream()
                        .map(param -> VariableManifest.of(param, context.processingEnvironment()))
                        .toList(),
                context.requestParameterBuilder());
        fileWriter.writeFile(polymorphic.packageName(), name, type);
    }

    private static String leafName(String simpleName) {
        var dotIndex = simpleName.lastIndexOf('.');
        return dotIndex >= 0 ? simpleName.substring(dotIndex + 1) : simpleName;
    }
}
