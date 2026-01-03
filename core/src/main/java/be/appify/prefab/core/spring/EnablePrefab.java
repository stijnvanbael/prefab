package be.appify.prefab.core.spring;

import be.appify.prefab.core.kafka.KafkaConfiguration;
import be.appify.prefab.core.pubsub.PubSubConfiguration;
import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Enable Prefab framework features in a Spring application. */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Import({ PrefabConfiguration.class, KafkaConfiguration.class, PubSubConfiguration.class })
public @interface EnablePrefab {
}
