package com.gfos.ideaboard.resource;

import com.gfos.ideaboard.dto.NotificationDTO;
import com.gfos.ideaboard.security.Secured;
import com.gfos.ideaboard.service.NotificationService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.Map;

@Path("/notifications")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Secured
public class NotificationResource {

    @Inject
    private NotificationService notificationService;

    @GET
    public Response getNotifications(
            @QueryParam("limit") @DefaultValue("50") int limit,
            @Context ContainerRequestContext requestContext) {
        Long userId = (Long) requestContext.getProperty("userId");
        List<NotificationDTO> notifications = notificationService.getNotificationsByUser(userId, limit);
        return Response.ok(notifications).build();
    }

    @GET
    @Path("/unread-count")
    public Response getUnreadCount(@Context ContainerRequestContext requestContext) {
        Long userId = (Long) requestContext.getProperty("userId");
        long count = notificationService.getUnreadCount(userId);
        return Response.ok(Map.of("count", count)).build();
    }

    @PUT
    @Path("/{id}/read")
    public Response markAsRead(@PathParam("id") Long id, @Context ContainerRequestContext requestContext) {
        Long userId = (Long) requestContext.getProperty("userId");
        notificationService.markAsRead(id, userId);
        return Response.ok(Map.of("message", "Als gelesen markiert")).build();
    }

    @PUT
    @Path("/read-all")
    public Response markAllAsRead(@Context ContainerRequestContext requestContext) {
        Long userId = (Long) requestContext.getProperty("userId");
        notificationService.markAllAsRead(userId);
        return Response.ok(Map.of("message", "Alle als gelesen markiert")).build();
    }
}
