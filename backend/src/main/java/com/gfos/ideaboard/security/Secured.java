package com.gfos.ideaboard.security;

import jakarta.ws.rs.NameBinding;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Markiert eine Ressource oder Methode als gesichert und erfordert JWT-Authentifizierung.
 * Der JwtFilter wird diese Annotation respektieren und Token validieren.
 */
@NameBinding
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Secured {
}
