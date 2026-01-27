package com.taskify.taskify.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark an API endpoint as deprecated with specific metadata
 * for HTTP headers (Sunset and Link).
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiDeprecated {
    /**
     * The date after which the endpoint may be removed (ISO-8601).
     */
    String sunsetDate();

    /**
     * The URL of the successor endpoint.
     */
    String successorUrl();
}
