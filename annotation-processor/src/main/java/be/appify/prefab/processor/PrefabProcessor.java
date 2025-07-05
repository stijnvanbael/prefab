package be.appify.prefab.processor;

import be.appify.prefab.core.annotations.Aggregate;
import com.google.auto.service.AutoService;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.Spliterators;
import java.util.stream.StreamSupport;

@SupportedAnnotationTypes({
        "be.appify.prefab.core.annotations.*",
})
@SupportedSourceVersion(SourceVersion.RELEASE_22)
@AutoService(Processor.class)
@SuppressWarnings("unused")
public class PrefabProcessor extends AbstractProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment environment) {
        var plugins = detectPlugins();
        var manifests = environment.getElementsAnnotatedWith(Aggregate.class)
                .stream()
                .filter(element -> element.getKind().isClass() && !element.getModifiers().contains(Modifier.ABSTRACT))
                .map(element -> new ClassManifest((TypeElement) element, processingEnv))
                .toList();
        var context = new PrefabContext(processingEnv, plugins, environment);
        manifests.forEach(manifest -> {
            new HttpWriter(context).writeHttpLayer(manifest);
            new ApplicationWriter(context).writeApplicationLayer(manifest);
            new PersistenceWriter(context).writePersistenceLayer(manifest);
        });
        plugins.forEach(plugin -> plugin.writeAdditionalFiles(manifests, context));
        return true;
    }

    private List<PrefabPlugin> detectPlugins() {
        var loader = ServiceLoader.load(PrefabPlugin.class, getClass().getClassLoader()).iterator();
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(loader, 0), false)
                .toList();
    }
}
