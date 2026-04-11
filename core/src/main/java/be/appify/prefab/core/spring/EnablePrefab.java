package be.appify.prefab.core.spring;

import be.appify.prefab.core.kafka.KafkaConfiguration;
import be.appify.prefab.core.pubsub.PubSubConfiguration;
import be.appify.prefab.core.sns.SnsConfiguration;
import be.appify.prefab.core.util.SerializationRegistry;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.context.annotation.Import;

/** Enable Prefab framework features in a Spring application. */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Import({ PrefabCoreConfiguration.class, AuditConfiguration.class, KafkaConfiguration.class, PubSubConfiguration.class, SnsConfiguration.class, SerializationRegistry.class })
public @interface EnablePrefab {
}
