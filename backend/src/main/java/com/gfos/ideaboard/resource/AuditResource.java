package com.gfos.ideaboard.resource;

import com.gfos.ideaboard.dto.AuditLogDTO;
import com.gfos.ideaboard.exception.ApiException;
import com.gfos.ideaboard.security.Secured;
import com.gfos.ideaboard.service.AuditService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;

@Path("/audit-logs")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Secured
public class AuditResource {

    @Inject
    private AuditService auditService;

    @GET
    public Response getAuditLogs(
            @QueryParam("limit") @DefaultValue("100") int limit,
            @Context ContainerRequestContext requestContext) {
        String role = (String) requestContext.getProperty("role");
        if (!"ADMIN".equals(role)) {
            throw ApiException.forbidden("Nur Administratoren können Audit-Protokolle anzeigen");
        }

        List<AuditLogDTO> logs = auditService.getRecentLogs(limit);
        return Response.ok(logs).build();
    }

    @GET
    @Path("/entity/{entityType}/{entityId}")
    public Response getAuditLogsByEntity(
            @PathParam("entityType") String entityType,
            @PathParam("entityId") Long entityId,
            @Context ContainerRequestContext requestContext) {
        String role = (String) requestContext.getProperty("role");
        if (!"ADMIN".equals(role)) {
            throw ApiException.forbidden("Nur Administratoren können Audit-Protokolle anzeigen");
        }

        List<AuditLogDTO> logs = auditService.getLogsByEntity(entityType, entityId);
        return Response.ok(logs).build();
    }
}
