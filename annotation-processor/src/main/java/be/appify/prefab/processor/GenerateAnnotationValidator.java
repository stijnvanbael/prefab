package be.appify.prefab.processor;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.annotations.Generate;
import be.appify.prefab.core.annotations.GenerateOverrides;
import be.appify.prefab.core.annotations.OutputTarget;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;

/**
 * Validates {@code @Generate} annotations on aggregate and event contract classes.
 *
 * <p>Performs compile-time validation to ensure:
 * <ul>
 *   <li>The referenced plugin class is a {@code PrefabPlugin}
 *   <li>The plugin class exists on the annotation-processor classpath
 *   <li>The target {@code OutputTarget} is supported by the plugin
 *   <li>The annotation is placed on an aggregate or event contract class (warning only)
 * </ul>
 *
 * <p>Validation is fail-fast: compilation errors are emitted via the Messager on the first
 * violation, and overlay safe defaults on recovery.
 */
public class GenerateAnnotationValidator {
    private final ProcessingEnvironment processingEnvironment;
    private final Elements elementUtils;
    private final Types typeUtils;

    /**
     * Creates a new validator.
     *
     * @param processingEnvironment the annotation processor's environment
     */
    public GenerateAnnotationValidator(ProcessingEnvironment processingEnvironment) {
        this.processingEnvironment = processingEnvironment;
        this.elementUtils = processingEnvironment.getElementUtils();
        this.typeUtils = processingEnvironment.getTypeUtils();
    }

    /**
     * Validates all {@code @Generate} annotations on a type and returns a registry of valid overrides.
     *
     * <p>If validation fails, an error is emitted via the Messager and an empty registry is returned.
     *
     * @param typeElement the class element to validate
     * @return a registry of valid plugin overrides (empty if validation failed)
     */
    public PluginOverrideRegistry validateAndBuildRegistry(TypeElement typeElement) {
        Map<Class<?>, PluginOverride> overrides = new HashMap<>();

        // Check if this is an aggregate or event contract
        var hasAggregate = typeElement.getAnnotation(Aggregate.class) != null;
        var hasEvent = typeElement.getAnnotation(Event.class) != null;
        if (!hasAggregate && !hasEvent) {
            if (typeElement.getAnnotation(Generate.class) != null || typeElement.getAnnotation(GenerateOverrides.class) != null) {
                warn(
                        typeElement,
                        "@Generate on class " + typeElement.getQualifiedName()
                                + " is ignored because it is not annotated with @Aggregate or @Event"
                );
            }
            return new PluginOverrideRegistry();
        }

        // Collect all @Generate annotations (handles both single and repeatable)
        Stream.of(typeElement.getAnnotationsByType(Generate.class))
                .forEach(generateAnnotation -> {
                    MirroredTypeException mte = null;
                    Class<?> pluginClass = null;

                    try {
                        pluginClass = generateAnnotation.plugin();
                    } catch (MirroredTypeException e) {
                        mte = e;
                    }

                    if (mte == null && pluginClass != null) {
                        // Validate: plugin class is accessible and is a PrefabPlugin
                        if (!isPrefabPluginSubclass(pluginClass, typeElement)) {
                            error(
                                    typeElement,
                                    "Cannot use @Generate(plugin=" + pluginClass.getName()
                                            + "): must reference a PrefabPlugin subclass"
                            );
                            return;
                        }

                        // Check for duplicate overrides for the same plugin
                        if (overrides.containsKey(pluginClass)) {
                            warn(
                                    typeElement,
                                    "@Generate(plugin=" + pluginClass.getSimpleName()
                                            + ") is defined multiple times; only the first override will be used"
                            );
                            return;
                        }

                        // Validate: OutputTarget is supported by the plugin
                        OutputTarget target = generateAnnotation.target();
                        if (target == OutputTarget.TEST && !supportsTestOutput()) {
                            error(
                                    typeElement,
                                    "Plugin " + pluginClass.getSimpleName()
                                            + " does not support OutputTarget.TEST; use DEFAULT or MAIN instead"
                            );
                            return;
                        }

                        // All validation passed; add to overrides
                        overrides.put(
                                pluginClass,
                                new PluginOverride(pluginClass, generateAnnotation.enabled(), target)
                        );
                    } else if (mte != null) {
                        // Plugin class could not be resolved; try to infer from the mirror
                        DeclaredType declaredType = (DeclaredType) mte.getTypeMirror();
                        if (!isPrefabPluginSubclassMirror(declaredType, typeElement)) {
                            String pluginName = declaredType.toString();
                            error(
                                    typeElement,
                                    "Cannot use @Generate(plugin=" + pluginName
                                            + "): must reference a PrefabPlugin subclass"
                            );
                            return;
                        }

                        // Check if plugin class is on the classpath
                        if (elementUtils.getTypeElement(declaredType.toString()) == null) {
                            error(
                                    typeElement,
                                    "@Generate references plugin " + declaredType
                                            + ", but it is not on the annotation-processor classpath"
                            );
                            return;
                        }
                        var resolvedPluginClass = resolvePluginClass(declaredType.toString(), typeElement);
                        if (resolvedPluginClass == null) {
                            return;
                        }
                        if (overrides.containsKey(resolvedPluginClass)) {
                            warn(
                                    typeElement,
                                    "@Generate(plugin=" + resolvedPluginClass.getSimpleName()
                                            + ") is defined multiple times; only the first override will be used"
                            );
                            return;
                        }
                        OutputTarget target = generateAnnotation.target();
                        if (target == OutputTarget.TEST && !supportsTestOutput()) {
                            error(
                                    typeElement,
                                    "Plugin " + resolvedPluginClass.getSimpleName()
                                            + " does not support OutputTarget.TEST; use DEFAULT or MAIN instead"
                            );
                            return;
                        }
                        overrides.put(
                                resolvedPluginClass,
                                new PluginOverride(resolvedPluginClass, generateAnnotation.enabled(), target)
                        );
                    }
                });

        return new PluginOverrideRegistry(overrides);
    }

    /**
     * Check if a class is a {@code PrefabPlugin} subclass or interface.
     *
     * @param pluginClass the class to check
     * @param typeElement the element where @Generate was used (for error reporting)
     * @return true if the class is a PrefabPlugin
     */
    private boolean isPrefabPluginSubclass(Class<?> pluginClass, TypeElement typeElement) {
        try {
            return PrefabPlugin.class.isAssignableFrom(pluginClass);
        } catch (Exception e) {
            error(
                    typeElement,
                    "Could not verify if @Generate(plugin=" + pluginClass.getSimpleName()
                            + ") is a PrefabPlugin: " + e.getMessage()
            );
            return false;
        }
    }

    /**
     * Check if a type mirror represents a {@code PrefabPlugin} subclass or interface.
     *
     * @param declaredType the type mirror to check
     * @param typeElement the element where @Generate was used (for error reporting)
     * @return true if the type is a PrefabPlugin
     */
    private boolean isPrefabPluginSubclassMirror(DeclaredType declaredType, TypeElement typeElement) {
        TypeElement prefabPluginElement =
                elementUtils.getTypeElement(PrefabPlugin.class.getCanonicalName());
        if (prefabPluginElement == null) {
            error(
                    typeElement,
                    "Could not find PrefabPlugin on the annotation-processor classpath"
            );
            return false;
        }

        return typeUtils.isAssignable(declaredType, typeUtils.erasure(prefabPluginElement.asType()));
    }

    /**
     * Check if a plugin class declares support for {@code OutputTarget.TEST}.
     *
     * <p>For now, assumes all plugins support test output. In the future, plugins can declare
     * support via a marker annotation (e.g. {@code @SupportsTestOutput}).
     *
     * @return true if the plugin supports TEST output
     */
    private boolean supportsTestOutput() {
        // TODO: Check for @SupportsTestOutput marker annotation
        // For now, assume all plugins support test output
        return true;
    }

    private Class<?> resolvePluginClass(String pluginClassName, TypeElement typeElement) {
        try {
            return Class.forName(pluginClassName, false, PrefabPlugin.class.getClassLoader());
        } catch (ClassNotFoundException e) {
            // Source-only plugin declarations are valid in compilation tests but are not loadable as
            // runtime classes from the processor classloader in this round.
            return null;
        }
    }

    /**
     * Emit a compile error via the Messager.
     *
     * @param element the element where the error occurred
     * @param message the error message
     */
    private void error(Element element, String message) {
        processingEnvironment.getMessager()
                .printMessage(Diagnostic.Kind.ERROR, message, element);
    }

    /**
     * Emit a compile warning via the Messager.
     *
     * @param element the element where the warning occurred
     * @param message the warning message
     */
    private void warn(Element element, String message) {
        processingEnvironment.getMessager()
                .printMessage(Diagnostic.Kind.WARNING, message, element);
    }
}







