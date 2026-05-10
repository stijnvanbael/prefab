package be.appify.prefab.processor.rest.update;

import be.appify.prefab.processor.ClassManifest;
import be.appify.prefab.processor.JavaFileWriter;
import be.appify.prefab.processor.PolymorphicAggregateManifest;
import be.appify.prefab.processor.RequestParameterBuilder;
import com.palantir.javapoet.AnnotationSpec;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.TypeSpec;
import java.util.List;
import java.util.Map;
import javax.lang.model.element.Modifier;

import static be.appify.prefab.processor.rest.ControllerUtil.writeRecord;
import static org.apache.commons.text.WordUtils.capitalize;

class UpdateRequestRecordWriter {
    void writeUpdateRequestRecord(
            JavaFileWriter fileWriter,
            ClassManifest manifest,
            UpdateManifest update,
            RequestParameterBuilder parameterBuilder,
            String builderSetterPrefix
    ) {
        var name = "%s%sRequest".formatted(manifest.simpleName(), capitalize(update.operationName()));
        var type = writeRecord(ClassName.get(manifest.packageName() + ".application", name), update.requestParameters(),
                parameterBuilder, builderSetterPrefix);
        fileWriter.writeFile(manifest.packageName(), name, type);
    }

    void writeUnionUpdateRequestInterface(
            JavaFileWriter fileWriter,
            PolymorphicAggregateManifest polymorphic,
            List<Map.Entry<ClassManifest, UpdateManifest>> group,
            RequestParameterBuilder parameterBuilder
    ) {
        var operationName = capitalize(group.getFirst().getValue().operationName());
        var unionName = "%s%sRequest".formatted(polymorphic.simpleName(), operationName);
        var unionClass = ClassName.get(polymorphic.packageName() + ".application", unionName);

        var permittedSubclasses = group.stream()
                .map(e -> nestedClass(polymorphic, unionName, e.getKey(), operationName))
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

        group.forEach(e -> outerInterface.addType(buildNestedRecord(unionClass, e, operationName, parameterBuilder)));

        fileWriter.writeFile(polymorphic.packageName(), unionName, outerInterface.build());
    }

    private TypeSpec buildNestedRecord(
            ClassName unionClass,
            Map.Entry<ClassManifest, UpdateManifest> entry,
            String operationName,
            RequestParameterBuilder parameterBuilder
    ) {
        var leafName = leafName(entry.getKey().simpleName());
        var nestedName = "%s%sRequest".formatted(leafName, operationName);
        var recordParams = entry.getValue().requestParameters().stream()
                .flatMap(p -> parameterBuilder.buildBodyParameter(p)
                        .or(() -> parameterBuilder.buildMethodParameter(p))
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

    private static ClassName nestedClass(PolymorphicAggregateManifest polymorphic, String unionName,
            ClassManifest subtype, String operationName) {
        return ClassName.get(polymorphic.packageName() + ".application", unionName,
                "%s%sRequest".formatted(leafName(subtype.simpleName()), operationName));
    }

    private static String leafName(String simpleName) {
        var dotIndex = simpleName.lastIndexOf('.');
        return dotIndex >= 0 ? simpleName.substring(dotIndex + 1) : simpleName;
    }
}
