package be.appify.prefab.processor.mongodb;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;

/**
 * Generates the type {@code mongodb.deferred.Stamp} in the first round, the way an AVSC processor generates event
 * records, so that an aggregate using it can only be resolved in a later round.
 */
class StampGeneratingProcessor extends AbstractProcessor {
    private boolean generated;

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Set.of("*");
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (generated) {
            return false;
        }
        generated = true;
        try (var writer = processingEnv.getFiler().createSourceFile("mongodb.deferred.Stamp").openWriter()) {
            writer.write("package mongodb.deferred;\npublic record Stamp(String value) {}\n");
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot generate Stamp", e);
        }
        return false;
    }
}
