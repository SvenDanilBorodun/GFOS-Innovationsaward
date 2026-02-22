package com.gfos.ideaboard.config;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;

/**
 * CORS-Filter, der Preflight-OPTIONS-Anfragen verarbeitet und CORS-Header zu allen Antworten hinzufügt.
 * Verwendet @PreMatching, um OPTIONS-Anfragen VOR dem Ausführen von Authentifizierungsfiltern abzufangen.
 */
@Provider
@PreMatching
@Priority(Priorities.HEADER_DECORATOR)
public class CorsFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final String ALLOWED_ORIGIN = "*";
    private static final String ALLOWED_METHODS = "GET, POST, PUT, DELETE, OPTIONS, HEAD, PATCH";
    private static final String ALLOWED_HEADERS = "Origin, Content-Type, Accept, Authorization, X-Requested-With";
    private static final String MAX_AGE = "86400";

    /**
     * Anfrage-Filter - verarbeitet OPTIONS-Preflight-Anfragen sofort ohne Authentifizierung.
     */
    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        // Verarbeiten Sie Preflight-OPTIONS-Anfragen - brechen Sie vor der Authentifizierung ab
        if ("OPTIONS".equalsIgnoreCase(requestContext.getMethod())) {
            requestContext.abortWith(
                Response.ok()
                    .header("Access-Control-Allow-Origin", ALLOWED_ORIGIN)
                    .header("Access-Control-Allow-Credentials", "true")
                    .header("Access-Control-Allow-Headers", ALLOWED_HEADERS)
                    .header("Access-Control-Allow-Methods", ALLOWED_METHODS)
                    .header("Access-Control-Max-Age", MAX_AGE)
                    .build()
            );
        }
    }

    /**
     * Antwort-Filter - fügt CORS-Header zu allen Nicht-OPTIONS-Antworten hinzu.
     */
    @Override
    public void filter(ContainerRequestContext requestContext,
                       ContainerResponseContext responseContext) throws IOException {
        // Fügen Sie CORS-Header zu allen Antworten hinzu
        responseContext.getHeaders().add("Access-Control-Allow-Origin", ALLOWED_ORIGIN);
        responseContext.getHeaders().add("Access-Control-Allow-Credentials", "true");
        responseContext.getHeaders().add("Access-Control-Allow-Headers", ALLOWED_HEADERS);
        responseContext.getHeaders().add("Access-Control-Allow-Methods", ALLOWED_METHODS);
        responseContext.getHeaders().add("Access-Control-Max-Age", MAX_AGE);
    }
}
