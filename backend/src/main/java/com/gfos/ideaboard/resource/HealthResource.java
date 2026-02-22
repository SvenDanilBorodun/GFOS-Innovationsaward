package com.gfos.ideaboard.resource;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;

/**
 * Health Check Endpunkt für Docker Container Health Checks.
 */
@Path("/health")
@Produces(MediaType.APPLICATION_JSON)
public class HealthResource {

    @PersistenceContext(unitName = "IdeaBoardPU")
    private EntityManager em;

    @GET
    public Response healthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("timestamp", System.currentTimeMillis());

        try {
            // Datenbankverbindung testen
            em.createNativeQuery("SELECT 1").getSingleResult();
            health.put("status", "UP");
            health.put("database", "connected");
            return Response.ok(health).build();
        } catch (Exception e) {
            health.put("status", "DOWN");
            health.put("database", "disconnected");
            health.put("error", e.getMessage());
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                .entity(health)
                .build();
        }
    }
}
