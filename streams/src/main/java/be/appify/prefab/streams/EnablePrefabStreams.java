package be.appify.prefab.streams;

import be.appify.prefab.streams.kafka.StreamsConfiguration;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.context.annotation.Import;

/** Enables Prefab streams DSL support. */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Import(StreamsConfiguration.class)
public @interface EnablePrefabStreams {
}

