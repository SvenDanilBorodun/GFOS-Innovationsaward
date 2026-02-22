package com.gfos.ideaboard.resource;

import com.gfos.ideaboard.exception.ApiException;
import com.gfos.ideaboard.security.Secured;
import com.gfos.ideaboard.service.ExportService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.IOException;

@Path("/export")
@Secured
public class ExportResource {

    @Inject
    private ExportService exportService;

    @GET
    @Path("/ideas/csv")
    @Produces("text/csv")
    public Response exportIdeasCsv(@Context ContainerRequestContext requestContext) {
        String role = (String) requestContext.getProperty("role");
        if (!"ADMIN".equals(role) && !"PROJECT_MANAGER".equals(role)) {
            throw ApiException.forbidden("Nur Administratoren und Projektmanager können Daten exportieren");
        }

        byte[] csv = exportService.exportIdeasToCsv();
        return Response.ok(csv)
                .header("Content-Disposition", "attachment; filename=\"ideas.csv\"")
                .build();
    }

    @GET
    @Path("/statistics/csv")
    @Produces("text/csv")
    public Response exportStatisticsCsv(@Context ContainerRequestContext requestContext) {
        String role = (String) requestContext.getProperty("role");
        if (!"ADMIN".equals(role) && !"PROJECT_MANAGER".equals(role)) {
            throw ApiException.forbidden("Nur Administratoren und Projektmanager können Daten exportieren");
        }

        byte[] csv = exportService.exportStatisticsToCsv();
        return Response.ok(csv)
                .header("Content-Disposition", "attachment; filename=\"statistics.csv\"")
                .build();
    }

    @GET
    @Path("/statistics/pdf")
    @Produces("application/pdf")
    public Response exportStatisticsPdf(@Context ContainerRequestContext requestContext) {
        String role = (String) requestContext.getProperty("role");
        if (!"ADMIN".equals(role) && !"PROJECT_MANAGER".equals(role)) {
            throw ApiException.forbidden("Nur Administratoren und Projektmanager können Daten exportieren");
        }

        try {
            byte[] pdf = exportService.exportStatisticsToPdf();
            return Response.ok(pdf)
                    .header("Content-Disposition", "attachment; filename=\"statistics.pdf\"")
                    .build();
        } catch (IOException e) {
            throw ApiException.serverError("Fehler beim Generieren des PDF: " + e.getMessage());
        }
    }

    @GET
    @Path("/users/csv")
    @Produces("text/csv")
    public Response exportUsersCsv(@Context ContainerRequestContext requestContext) {
        String role = (String) requestContext.getProperty("role");
        if (!"ADMIN".equals(role)) {
            throw ApiException.forbidden("Nur Administratoren können Benutzerdaten exportieren");
        }

        byte[] csv = exportService.exportUsersToCsv();
        return Response.ok(csv)
                .header("Content-Disposition", "attachment; filename=\"users.csv\"")
                .build();
    }
}
