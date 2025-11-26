package be.appify.prefab.core.annotations.rest;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.SOURCE)
public @interface Security {
    boolean enabled() default true;

    String authority() default "";
}
