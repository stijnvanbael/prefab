package be.appify.prefab.processor.spring;

import java.util.function.Supplier;

public final class RepositorySupport {

    public static void handleErrors(Runnable call) {
        handleErrors(() -> {
            call.run();
            return null;
        });
    }

    public static <T> T handleErrors(Supplier<T> call) {
        try {
            return call.get();
        } catch (IllegalStateException e) {
            if (e.getMessage().startsWith("Required identifier field not found for class ")) {
                throw new IllegalStateException(
                        e.getMessage() + ". This usually indicates a compilation problem. Try cleaning the output folder an running a full compile again (eg. mvn clean compile)",
                        e);
            }
            throw e;
        }
    }
}
