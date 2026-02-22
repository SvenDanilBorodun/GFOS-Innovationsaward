package com.gfos.ideaboard.security;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;
import java.security.Principal;

@Provider
@Secured
@Priority(Priorities.AUTHENTICATION)
public class JwtFilter implements ContainerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    @Inject
    private JwtUtil jwtUtil;

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        String authHeader = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            abortWithUnauthorized(requestContext, "Autorisierungs-Header fehlt oder ist ungültig");
            return;
        }

        String token = authHeader.substring(BEARER_PREFIX.length()).trim();

        if (!jwtUtil.isTokenValid(token)) {
            abortWithUnauthorized(requestContext, "Ungültiger oder abgelaufener Token");
            return;
        }

        // Akzeptieren Sie keine Aktualisierungs-Token für reguläre API-Aufrufe
        if (jwtUtil.isRefreshToken(token)) {
            abortWithUnauthorized(requestContext, "Aktualisierungs-Token wird nicht akzeptiert");
            return;
        }

        Long userId = jwtUtil.getUserIdFromToken(token);
        String username = jwtUtil.getUsernameFromToken(token);
        String role = jwtUtil.getRoleFromToken(token);

        // Sicherheitskontext einstellen
        requestContext.setSecurityContext(new SecurityContext() {
            @Override
            public Principal getUserPrincipal() {
                return () -> username;
            }

            @Override
            public boolean isUserInRole(String r) {
                return role != null && role.equals(r);
            }

            @Override
            public boolean isSecure() {
                return requestContext.getSecurityContext().isSecure();
            }

            @Override
            public String getAuthenticationScheme() {
                return "Bearer";
            }
        });

        // Benutzerinformationen in Anfrageeigenschaften speichern zur späteren Verwendung
        requestContext.setProperty("userId", userId);
        requestContext.setProperty("username", username);
        requestContext.setProperty("role", role);
    }

    private void abortWithUnauthorized(ContainerRequestContext requestContext, String message) {
        requestContext.abortWith(
            Response.status(Response.Status.UNAUTHORIZED)
                .entity("{\"error\": \"" + message + "\"}")
                .type("application/json")
                .build()
        );
    }
}
