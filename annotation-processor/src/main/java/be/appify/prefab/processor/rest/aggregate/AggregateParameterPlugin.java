package be.appify.prefab.processor.rest.aggregate;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.core.annotations.rest.Update;
import be.appify.prefab.processor.ClassManifest;
import be.appify.prefab.processor.PrefabContext;
import be.appify.prefab.processor.PrefabPlugin;
import be.appify.prefab.processor.PolymorphicAggregateManifest;
import be.appify.prefab.processor.VariableManifest;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.ParameterSpec;
import com.palantir.javapoet.TypeName;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.lang.model.element.ExecutableElement;

import static org.apache.commons.text.WordUtils.uncapitalize;

/**
 * Plugin that resolves {@link Aggregate}-typed parameters in {@link Create} and {@link Update} generated
 * service methods by fetching them from their respective repositories.
 */
public class AggregateParameterPlugin implements PrefabPlugin {

    private PrefabContext context;

    @Override
    public void initContext(PrefabContext context) {
        this.context = context;
    }

    @Override
    public Optional<ParameterSpec> requestBodyParameter(VariableManifest parameter) {
        if (!isAggregateTyped(parameter)) {
            return Optional.empty();
        }
        return Optional.of(ParameterSpec.builder(String.class, parameter.name() + "Id").build());
    }

    @Override
    public Optional<CodeBlock> mapRequestParameter(VariableManifest parameter) {
        if (!isAggregateTyped(parameter)) {
            return Optional.empty();
        }
        var repositoryName = uncapitalize(topLevelName(parameter.type().simpleName())) + "Repository";
        return Optional.of(CodeBlock.of("$N.findById(request.$NId()).orElseThrow()",
                repositoryName, parameter.name()));
    }

    @Override
    public Set<TypeName> getServiceDependencies(ClassManifest manifest) {
        return aggregateTypedParamsOf(manifest)
                .map(this::repositoryTypeFor)
                .collect(Collectors.toSet());
    }

    @Override
    public Set<TypeName> getPolymorphicServiceDependencies(PolymorphicAggregateManifest manifest) {
        return manifest.subtypes().stream()
                .flatMap(this::aggregateTypedParamsOf)
                .map(this::repositoryTypeFor)
                .collect(Collectors.toSet());
    }

    private Stream<VariableManifest> aggregateTypedParamsOf(ClassManifest manifest) {
        var createParams = manifest.constructorsWith(Create.class).stream()
                .flatMap(constructor -> parametersOf(constructor).stream());
        var updateParams = manifest.methodsWith(Update.class).stream()
                .flatMap(method -> parametersOf(method).stream());
        return Stream.concat(createParams, updateParams)
                .filter(this::isAggregateTyped);
    }

    private TypeName repositoryTypeFor(VariableManifest parameter) {
        var paramPackage = parameter.type().packageName();
        return ClassName.get(paramPackage + ".application",
                topLevelName(parameter.type().simpleName()) + "Repository");
    }

    private static String topLevelName(String simpleName) {
        var dotIndex = simpleName.indexOf('.');
        return dotIndex >= 0 ? simpleName.substring(0, dotIndex) : simpleName;
    }

    boolean isAggregateTyped(VariableManifest parameter) {
        return !parameter.type().annotationsOfType(Aggregate.class).isEmpty();
    }

    private List<VariableManifest> parametersOf(ExecutableElement method) {
        return method.getParameters().stream()
                .map(param -> VariableManifest.of(param, context.processingEnvironment()))
                .toList();
    }
}






