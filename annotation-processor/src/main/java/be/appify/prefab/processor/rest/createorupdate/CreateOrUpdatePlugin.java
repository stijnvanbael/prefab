package be.appify.prefab.processor.rest.createorupdate;

import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.core.annotations.rest.Update;
import be.appify.prefab.processor.ClassManifest;
import be.appify.prefab.processor.JavaFileWriter;
import be.appify.prefab.processor.PrefabContext;
import be.appify.prefab.processor.PrefabPlugin;
import be.appify.prefab.processor.VariableManifest;
import be.appify.prefab.processor.rest.PathVariables;
import be.appify.prefab.processor.rest.create.CreateRequestRecordWriter;
import be.appify.prefab.processor.rest.update.UpdateManifest;
import com.palantir.javapoet.TypeSpec;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ExecutableElement;

/**
 * Plugin that detects create-or-update pairs (a {@code @Create} constructor/factory sharing the same HTTP method and
 * URL as an {@code @Update} method) and generates a single combined endpoint for them.
 *
 * <p>Pairing rule: a {@code @Create} and {@code @Update} are paired when {@code @Create.method == @Update.method}
 * AND {@code @Create.path} starts with a single path variable segment that equals the implicit {@code /{id}} prefix
 * of the update endpoint, and the remainder of the path matches {@code @Update.path}.
 */
public class CreateOrUpdatePlugin implements PrefabPlugin {
    private static final Pattern LEADING_PATH_VAR = Pattern.compile("^/\\{(\\w+)}(.*)$");

    private final CreateOrUpdateControllerWriter controllerWriter = new CreateOrUpdateControllerWriter();
    private final CreateOrUpdateServiceWriter serviceWriter = new CreateOrUpdateServiceWriter();
    private final CreateOrUpdateTestClientWriter testClientWriter = new CreateOrUpdateTestClientWriter();
    private final CreateRequestRecordWriter requestRecordWriter = new CreateRequestRecordWriter();
    private final java.util.Map<ClassManifest, List<CreateOrUpdateManifest>> pairsCache =
            Collections.synchronizedMap(new WeakHashMap<>());
    private PrefabContext context;

    @Override
    public void initContext(PrefabContext context) {
        this.context = context;
    }

    @Override
    public void writeController(ClassManifest manifest, TypeSpec.Builder builder) {
        createOrUpdatePairsOf(manifest).forEach(pair ->
                builder.addMethod(controllerWriter.createOrUpdateMethod(manifest, pair, context)));
    }

    @Override
    public void writeService(ClassManifest manifest, TypeSpec.Builder builder) {
        createOrUpdatePairsOf(manifest).forEach(pair ->
                builder.addMethod(serviceWriter.createOrUpdateMethod(manifest, pair, context)));
    }

    @Override
    public void writeTestClient(ClassManifest manifest, TypeSpec.Builder builder) {
        createOrUpdatePairsOf(manifest).forEach(pair ->
                testClientWriter.createOrUpdateMethods(manifest, pair, context).forEach(builder::addMethod));
    }

    @Override
    public void writeAdditionalFiles(List<ClassManifest> manifests) {
        if (!manifests.isEmpty()) {
            var fileWriter = new JavaFileWriter(context.processingEnvironment(), "application");
            manifests.forEach(manifest ->
                    createOrUpdatePairsOf(manifest).forEach(pair -> {
                        var create = pair.createConstructor().getAnnotation(Create.class);
                        var pathVarNames = PathVariables.extractFrom(create.path());
                        var params = pair.createConstructor().getParameters().stream()
                                .map(p -> VariableManifest.of(p, context.processingEnvironment()))
                                .toList();
                        var hasBodyParams = params.stream()
                                .anyMatch(p -> !pathVarNames.contains(p.name()));
                        if (hasBodyParams) {
                            requestRecordWriter.writeRequestRecord(fileWriter, manifest, pair.createConstructor(), context);
                        }
                    }));
        }
    }

    /**
     * Returns all create-or-update pairs for the given manifest.
     */
    List<CreateOrUpdateManifest> createOrUpdatePairsOf(ClassManifest manifest) {
        return pairsCache.computeIfAbsent(manifest, m -> buildPairs(m, context));
    }

    private List<CreateOrUpdateManifest> buildPairs(ClassManifest manifest, PrefabContext context) {
        var updateMethods = updateMethodsOf(manifest, context);
        return manifest.constructorsWith(Create.class).stream()
                .flatMap(ctor -> findPairedUpdate(ctor, updateMethods).stream()
                        .map(pair -> new CreateOrUpdateManifest(ctor, pair.update(), pair.lookupVariable())))
                .toList();
    }

    private List<UpdatePair> findPairedUpdate(ExecutableElement ctor, List<UpdateEntry> updateMethods) {
        var create = ctor.getAnnotation(Create.class);
        var matcher = LEADING_PATH_VAR.matcher(create.path());
        if (!matcher.matches()) {
            return List.of();
        }
        var lookupVariable = matcher.group(1);
        var pathSuffix = matcher.group(2);
        return updateMethods.stream()
                .filter(entry -> entry.update().method().equals(create.method()))
                .filter(entry -> entry.update().path().equals(pathSuffix))
                .map(entry -> new UpdatePair(entry.update(), lookupVariable))
                .toList();
    }

    private List<UpdateEntry> updateMethodsOf(ClassManifest manifest, PrefabContext context) {
        return manifest.methodsWith(Update.class).stream()
                .map(method -> new UpdateEntry(method, buildUpdateManifest(method, manifest, context)))
                .toList();
    }

    private UpdateManifest buildUpdateManifest(ExecutableElement method, ClassManifest manifest,
            PrefabContext context) {
        var update = method.getAnnotationsByType(Update.class)[0];
        var allParams = method.getParameters().stream()
                .map(p -> VariableManifest.of(p, context.processingEnvironment()))
                .toList();
        var pathVarNames = PathVariables.extractFrom(update.path());
        var parentField = manifest.parent();
        var parentFieldName = parentField.map(VariableManifest::name);
        var pathParameters = allParams.stream()
                .filter(p -> pathVarNames.contains(p.name()))
                .toList();
        var requestParameters = allParams.stream()
                .filter(p -> parentFieldName.map(pfn -> !pfn.equals(p.name())).orElse(true))
                .filter(p -> !pathVarNames.contains(p.name()))
                .toList();
        return new UpdateManifest(
                method.getSimpleName().toString(),
                allParams,
                requestParameters,
                pathParameters,
                List.of(),
                List.of(),
                method.getReturnType().toString().equals("void"),
                false,
                update.method(),
                update.path(),
                update.security());
    }

    private record UpdateEntry(ExecutableElement method, UpdateManifest update) {}

    private record UpdatePair(UpdateManifest update, String lookupVariable) {}
}
