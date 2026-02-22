package com.gfos.ideaboard.resource;

import com.gfos.ideaboard.dto.ConversationDTO;
import com.gfos.ideaboard.dto.MessageDTO;
import com.gfos.ideaboard.dto.SendMessageRequest;
import com.gfos.ideaboard.security.Secured;
import com.gfos.ideaboard.service.MessageService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.Map;

@Path("/messages")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Secured
public class MessageResource {

    @Inject
    private MessageService messageService;

    /**
     * Sende eine neue Nachricht an einen anderen Benutzer
     */
    @POST
    public Response sendMessage(
            @Valid SendMessageRequest request,
            @Context ContainerRequestContext requestContext) {
        Long senderId = (Long) requestContext.getProperty("userId");
        MessageDTO message = messageService.sendMessage(
                senderId,
                request.getRecipientId(),
                request.getContent(),
                request.getIdeaId()
        );
        return Response.status(Response.Status.CREATED).entity(message).build();
    }

    /**
     * Rufe alle Konversationen des aktuellen Benutzers ab
     */
    @GET
    @Path("/conversations")
    public Response getConversations(@Context ContainerRequestContext requestContext) {
        Long userId = (Long) requestContext.getProperty("userId");
        List<ConversationDTO> conversations = messageService.getUserConversations(userId);
        return Response.ok(conversations).build();
    }

    /**
     * Rufe Nachrichten in einer Konversation mit einem anderen Benutzer ab
     */
    @GET
    @Path("/conversations/{userId}")
    public Response getConversation(
            @PathParam("userId") Long otherUserId,
            @QueryParam("limit") @DefaultValue("50") int limit,
            @QueryParam("offset") @DefaultValue("0") int offset,
            @Context ContainerRequestContext requestContext) {
        Long userId = (Long) requestContext.getProperty("userId");
        List<MessageDTO> messages = messageService.getConversation(userId, otherUserId, limit, offset);
        return Response.ok(messages).build();
    }

    /**
     * Rufe die Anzahl ungelesener Nachrichten ab
     */
    @GET
    @Path("/unread-count")
    public Response getUnreadCount(@Context ContainerRequestContext requestContext) {
        Long userId = (Long) requestContext.getProperty("userId");
        long count = messageService.getUnreadCount(userId);
        return Response.ok(Map.of("count", count)).build();
    }

    /**
     * Markiere eine einzelne Nachricht als gelesen
     */
    @PUT
    @Path("/{id}/read")
    public Response markAsRead(
            @PathParam("id") Long messageId,
            @Context ContainerRequestContext requestContext) {
        Long userId = (Long) requestContext.getProperty("userId");
        messageService.markAsRead(messageId, userId);
        return Response.ok(Map.of("message", "Als gelesen markiert")).build();
    }

    /**
     * Markiere alle Nachrichten in einer Konversation als gelesen
     */
    @PUT
    @Path("/conversations/{userId}/read")
    public Response markConversationAsRead(
            @PathParam("userId") Long otherUserId,
            @Context ContainerRequestContext requestContext) {
        Long userId = (Long) requestContext.getProperty("userId");
        messageService.markConversationAsRead(userId, otherUserId);
        return Response.ok(Map.of("message", "Konversation als gelesen markiert")).build();
    }

    /**
     * Rufe Nachrichten zu einer bestimmten Idee ab
     */
    @GET
    @Path("/idea/{ideaId}")
    public Response getMessagesByIdea(
            @PathParam("ideaId") Long ideaId,
            @QueryParam("limit") @DefaultValue("50") int limit,
            @Context ContainerRequestContext requestContext) {
        List<MessageDTO> messages = messageService.getMessagesByIdea(ideaId, limit);
        return Response.ok(messages).build();
    }
}
