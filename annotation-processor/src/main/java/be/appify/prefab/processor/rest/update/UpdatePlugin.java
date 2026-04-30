package be.appify.prefab.processor.rest.update;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.AsyncCommit;
import be.appify.prefab.core.annotations.rest.Update;
import be.appify.prefab.processor.ClassManifest;
import be.appify.prefab.processor.JavaFileWriter;
import be.appify.prefab.processor.PolymorphicAggregateManifest;
import be.appify.prefab.processor.PrefabContext;
import be.appify.prefab.processor.PrefabPlugin;
import be.appify.prefab.processor.VariableManifest;
import be.appify.prefab.processor.rest.PathVariables;
import com.palantir.javapoet.TypeSpec;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;

import static org.apache.commons.text.WordUtils.capitalize;

/**
 * Plugin responsible for generating update controller and service methods, as well as related request records and test
 * client methods.
 */
public class UpdatePlugin implements PrefabPlugin {
    private final UpdateControllerWriter updateControllerWriter = new UpdateControllerWriter();
    private final UpdateServiceWriter updateServiceWriter = new UpdateServiceWriter();
    private final UpdateRequestRecordWriter updateRequestRecordWriter = new UpdateRequestRecordWriter();
    private final UpdateTestClientWriter updateTestClientWriter = new UpdateTestClientWriter();
    private PrefabContext context;

    @Override
    public void initContext(PrefabContext context) {
        this.context = context;
    }

    @Override
    public void writeController(ClassManifest manifest, TypeSpec.Builder builder) {
        updateMethodsOf(manifest).forEach(update ->
                builder.addMethod(updateControllerWriter.updateMethod(manifest, update, context)));
    }

    @Override
    public void writeService(ClassManifest manifest, TypeSpec.Builder builder) {
        updateMethodsOf(manifest).forEach(update ->
                builder.addMethod(updateServiceWriter.updateMethod(manifest, update)));
    }

    @Override
    public void writeAdditionalFiles(List<ClassManifest> manifests) {
        if (!manifests.isEmpty()) {
            var fileWriter = new JavaFileWriter(context.processingEnvironment(), "application");
            manifests.forEach(manifest -> updateMethodsOf(manifest).forEach(update -> {
                if (!update.requestParameters().isEmpty()) {
                    updateRequestRecordWriter.writeUpdateRequestRecord(fileWriter, manifest, update,
                            context.requestParameterBuilder());
                }
            }));
        }
    }

    @Override
    public void writeAdditionalFiles(List<ClassManifest> manifests, List<PolymorphicAggregateManifest> polymorphicManifests) {
        writeAdditionalFiles(manifests);
        if (!polymorphicManifests.isEmpty()) {
            var fileWriter = new JavaFileWriter(context.processingEnvironment(), "application");
            polymorphicManifests.forEach(polymorphic -> writePolymorphicUpdateAdditionalFiles(fileWriter, polymorphic));
        }
    }

    private void writePolymorphicUpdateAdditionalFiles(JavaFileWriter fileWriter, PolymorphicAggregateManifest polymorphic) {
        var grouped = groupSubtypesByPath(polymorphic);
        grouped.forEach((pathKey, entries) -> {
            if (isUnionGroup(entries)) {
                entries.forEach(e -> {
                    if (!e.getValue().requestParameters().isEmpty()) {
                        writePolymorphicUpdateRequestRecord(fileWriter, polymorphic, e.getKey(), e.getValue());
                    }
                });
                updateRequestRecordWriter.writeUnionUpdateRequestInterface(fileWriter, polymorphic, entries,
                        context.requestParameterBuilder());
            } else {
                entries.forEach(e -> {
                    if (!e.getValue().requestParameters().isEmpty()) {
                        writePolymorphicUpdateRequestRecord(fileWriter, polymorphic, e.getKey(), e.getValue());
                    }
                });
            }
        });
    }

    private void writePolymorphicUpdateRequestRecord(
            JavaFileWriter fileWriter,
            PolymorphicAggregateManifest polymorphic,
            ClassManifest subtype,
            UpdateManifest update
    ) {
        var leafName = leafName(subtype.simpleName());
        var name = "%s%sRequest".formatted(leafName, capitalize(update.operationName()));
        var type = be.appify.prefab.processor.rest.ControllerUtil.writeRecord(
                com.palantir.javapoet.ClassName.get(polymorphic.packageName() + ".application", name),
                update.requestParameters(),
                context.requestParameterBuilder());
        fileWriter.writeFile(polymorphic.packageName(), name, type);
    }

    @Override
    public void writeTestClient(ClassManifest manifest, TypeSpec.Builder builder) {
        updateMethodsOf(manifest).forEach(update ->
                updateTestClientWriter.updateMethods(manifest, update, context).forEach(builder::addMethod));
    }

    @Override
    public void writePolymorphicController(PolymorphicAggregateManifest manifest, TypeSpec.Builder builder) {
        var grouped = groupSubtypesByPath(manifest);
        grouped.forEach((pathKey, entries) -> {
            if (isUnionGroup(entries)) {
                builder.addMethod(updateControllerWriter.updateDispatchMethodForPolymorphic(manifest, entries));
            } else {
                entries.forEach(e -> builder.addMethod(
                        updateControllerWriter.updateMethodForPolymorphic(manifest, e.getKey(), e.getValue(), context)));
            }
        });
    }

    @Override
    public void writePolymorphicService(PolymorphicAggregateManifest manifest, TypeSpec.Builder builder) {
        manifest.subtypes().forEach(subtype ->
                updateMethodsOf(subtype).forEach(update ->
                        builder.addMethod(updateServiceWriter.updateMethodForPolymorphic(manifest, subtype, update))));
    }

    @Override
    public void writePolymorphicTestClient(PolymorphicAggregateManifest manifest, TypeSpec.Builder builder) {
        var grouped = groupSubtypesByPath(manifest);
        grouped.forEach((pathKey, entries) -> {
            if (isUnionGroup(entries)) {
                builder.addMethod(updateTestClientWriter.baseUpdateMethodForPolymorphic(manifest, entries.getFirst().getValue()));
                entries.forEach(e -> updateTestClientWriter.updateMethodsForPolymorphicUnion(manifest, e, context)
                        .forEach(builder::addMethod));
            } else {
                entries.forEach(e -> updateTestClientWriter.updateMethodsForPolymorphic(manifest, e.getKey(), e.getValue(), context)
                        .forEach(builder::addMethod));
            }
        });
    }

    private Map<String, List<Map.Entry<ClassManifest, UpdateManifest>>> groupSubtypesByPath(
            PolymorphicAggregateManifest manifest
    ) {
        return manifest.subtypes().stream()
                .flatMap(subtype -> updateMethodsOf(subtype).stream()
                        .map(update -> Map.entry(subtype, update)))
                .collect(Collectors.groupingBy(
                        e -> e.getValue().method() + "|" + e.getValue().path() + "|" + e.getValue().operationName(),
                        LinkedHashMap::new,
                        Collectors.toList()));
    }

    private static boolean isUnionGroup(List<Map.Entry<ClassManifest, UpdateManifest>> entries) {
        return entries.size() >= 2 && entries.stream().anyMatch(e -> !e.getValue().requestParameters().isEmpty());
    }

    private static String leafName(String simpleName) {
        var dotIndex = simpleName.lastIndexOf('.');
        return dotIndex >= 0 ? simpleName.substring(dotIndex + 1) : simpleName;
    }

    private List<UpdateManifest> updateMethodsOf(ClassManifest manifest) {
        var parentField = manifest.parent();
        var parentFieldName = parentField.map(VariableManifest::name);
        return manifest.methodsWith(Update.class).stream()
                .map(element -> {
                    var update = element.getAnnotationsByType(Update.class)[0];
                    var allParams = getParametersOf(element, context.processingEnvironment());
                    var pathVarNames = PathVariables.extractFrom(update.path());
                    var pathParameters = allParams.stream()
                            .filter(p -> pathVarNames.contains(p.name()))
                            .toList();
                    var requestParameters = allParams.stream()
                            .filter(p -> parentFieldName.map(pfn -> !pfn.equals(p.name())).orElse(true))
                            .filter(p -> !pathVarNames.contains(p.name()))
                            .toList();
                    var aggregateParams = allParams.stream()
                            .filter(p -> !p.type().annotationsOfType(Aggregate.class).isEmpty())
                            .toList();
                    var parentEntityParams = resolveParentEntityParameters(allParams, parentField);
                    return new UpdateManifest(
                            element.getSimpleName().toString(),
                            allParams,
                            requestParameters,
                            pathParameters,
                            aggregateParams,
                            parentEntityParams,
                            element.getReturnType().toString().equals("void"),
                            manifest.isAsyncCommit()
                                    || element.getAnnotationsByType(AsyncCommit.class).length > 0,
                            update.method(),
                            update.path(),
                            update.security());
                })
                .toList();
    }

    private List<VariableManifest> resolveParentEntityParameters(
            List<VariableManifest> allParams,
            Optional<VariableManifest> parentField
    ) {
        if (parentField.isEmpty()) {
            return List.of();
        }
        var field = parentField.get();
        return allParams.stream()
                .filter(p -> p.name().equals(field.name()))
                .filter(p -> !p.type().simpleName().equals(field.type().simpleName()))
                .toList();
    }

    private List<VariableManifest> getParametersOf(Element method,
            ProcessingEnvironment processingEnvironment) {
        return ((ExecutableElement) method).getParameters()
                .stream()
                .map(element -> VariableManifest.of(element, processingEnvironment))
                .toList();
    }
}
