package com.gfos.ideaboard.resource;

import com.gfos.ideaboard.security.Secured;
import com.gfos.ideaboard.service.CommentService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Map;

@Path("/comments")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Secured
public class CommentResource {

    @Inject
    private CommentService commentService;

    @DELETE
    @Path("/{id}")
    public Response deleteComment(@PathParam("id") Long id, @Context ContainerRequestContext requestContext) {
        Long userId = (Long) requestContext.getProperty("userId");
        commentService.deleteComment(id, userId);
        return Response.noContent().build();
    }

    @POST
    @Path("/{id}/reactions")
    public Response addReaction(@PathParam("id") Long id, Map<String, String> body,
                                @Context ContainerRequestContext requestContext) {
        Long userId = (Long) requestContext.getProperty("userId");
        String emoji = body.get("emoji");
        if (emoji == null || emoji.isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Emoji ist erforderlich"))
                    .build();
        }
        commentService.addReaction(id, emoji, userId);
        return Response.ok(Map.of("message", "Reaktion hinzugefügt")).build();
    }

    @DELETE
    @Path("/{id}/reactions/{emoji}")
    public Response removeReaction(@PathParam("id") Long id, @PathParam("emoji") String emoji,
                                   @Context ContainerRequestContext requestContext) {
        Long userId = (Long) requestContext.getProperty("userId");
        commentService.removeReaction(id, emoji, userId);
        return Response.ok(Map.of("message", "Reaktion entfernt")).build();
    }
}
