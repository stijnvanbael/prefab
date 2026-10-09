package be.appify.prefab.core.spring;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.context.annotation.Import;

/** Enable Prefab framework features in a Spring application. */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Import(PrefabCoreConfiguration.class)
public @interface EnablePrefab {
}
