package be.appify.prefab.processor;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.NoSuchFileException;
import java.util.List;
import java.util.Optional;
import javax.annotation.processing.Filer;
import javax.tools.StandardLocation;

/** Resolves application resources even when javac isolates the annotation processor's class loader. */
public final class CompilerResources {
    private CompilerResources() {
    }

    /**
     * Looks first in this module's copied resources, then its compile classpath. The processor
     * loader remains a fallback for builds supplying schemas as processor-path dependencies.
     * The caller owns the returned stream.
     *
     * @param filer the compiler's file manager facade
     * @param path a classpath-relative resource name
     * @param loader the plugin class loader
     * @return an open stream, or empty if the resource wasn't found
     * @throws IOException when a resource exists but cannot be read
     */
    public static Optional<InputStream> open(Filer filer, String path, ClassLoader loader) throws IOException {
        for (var location : List.of(StandardLocation.CLASS_OUTPUT, StandardLocation.CLASS_PATH)) {
            try {
                return Optional.of(filer.getResource(location, "", path).openInputStream());
            } catch (FileNotFoundException | NoSuchFileException missing) {
                // An absent entry at one location doesn't rule out the next.
            }
        }
        return Optional.ofNullable(loader.getResourceAsStream(path));
    }
}
