package com.gfos.ideaboard.resource;

import com.gfos.ideaboard.dto.BadgeDTO;
import com.gfos.ideaboard.dto.UserBadgeDTO;
import com.gfos.ideaboard.dto.UserDTO;
import com.gfos.ideaboard.entity.UserRole;
import com.gfos.ideaboard.exception.ApiException;
import com.gfos.ideaboard.security.Secured;
import com.gfos.ideaboard.service.GamificationService;
import com.gfos.ideaboard.service.LikeService;
import com.gfos.ideaboard.service.UserService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.container.ContainerRequestContext;
import java.util.List;
import java.util.Map;

@Path("/users")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UserResource {

    @Inject
    private UserService userService;

    @Inject
    private LikeService likeService;

    @Inject
    private GamificationService gamificationService;

    @GET
    @Path("/me")
    @Secured
    public Response getCurrentUser(@Context ContainerRequestContext requestContext) {
        Long userId = (Long) requestContext.getProperty("userId");
        UserDTO user = userService.getUserById(userId);
        return Response.ok(user).build();
    }

    @PUT
    @Path("/me")
    @Secured
    public Response updateCurrentUser(@Context ContainerRequestContext requestContext, Map<String, String> body) {
        Long userId = (Long) requestContext.getProperty("userId");
        UserDTO updated = userService.updateUser(
                userId,
                body.get("firstName"),
                body.get("lastName"),
                body.get("email")
        );
        return Response.ok(updated).build();
    }

    @PUT
    @Path("/me/password")
    @Secured
    public Response changePassword(@Context ContainerRequestContext requestContext, Map<String, String> body) {
        Long userId = (Long) requestContext.getProperty("userId");
        String oldPassword = body.get("oldPassword");
        String newPassword = body.get("newPassword");

        if (oldPassword == null || newPassword == null) {
            throw ApiException.badRequest("Altes und neues Passwort sind erforderlich");
        }

        userService.changePassword(userId, oldPassword, newPassword);
        return Response.ok(Map.of("message", "Passwort erfolgreich geändert")).build();
    }

    @GET
    @Path("/me/likes/remaining")
    @Secured
    public Response getRemainingLikes(@Context ContainerRequestContext requestContext) {
        Long userId = (Long) requestContext.getProperty("userId");
        int remaining = likeService.getRemainingLikes(userId);
        int used = likeService.getWeeklyLikesUsed(userId);
        return Response.ok(Map.of(
                "remainingLikes", remaining,
                "weeklyLikesUsed", used,
                "maxWeeklyLikes", 3
        )).build();
    }

    @GET
    @Secured
    public Response getAllUsers(@Context ContainerRequestContext requestContext) {
        String role = (String) requestContext.getProperty("role");
        if (!"ADMIN".equals(role)) {
            throw ApiException.forbidden("Nur Administratoren können alle Benutzer auflisten");
        }
        List<UserDTO> users = userService.getAllUsers();
        return Response.ok(users).build();
    }

    @GET
    @Path("/{id}")
    @Secured
    public Response getUserById(@PathParam("id") Long id, @Context ContainerRequestContext requestContext) {
        String role = (String) requestContext.getProperty("role");
        if (!"ADMIN".equals(role)) {
            throw ApiException.forbidden("Nur Administratoren können andere Benutzer anzeigen");
        }
        UserDTO user = userService.getUserById(id);
        return Response.ok(user).build();
    }

    @PUT
    @Path("/{id}/role")
    @Secured
    public Response updateRole(@PathParam("id") Long id, Map<String, String> body,
                               @Context ContainerRequestContext requestContext) {
        String role = (String) requestContext.getProperty("role");
        if (!"ADMIN".equals(role)) {
            throw ApiException.forbidden("Nur Administratoren können Rollen ändern");
        }

        String newRole = body.get("role");
        if (newRole == null) {
            throw ApiException.badRequest("Rolle ist erforderlich");
        }

        try {
            UserRole userRole = UserRole.valueOf(newRole);
            userService.updateRole(id, userRole);
            return Response.ok(Map.of("message", "Rolle aktualisiert")).build();
        } catch (IllegalArgumentException e) {
            throw ApiException.badRequest("Ungültige Rolle");
        }
    }

    @PUT
    @Path("/{id}/status")
    @Secured
    public Response updateStatus(@PathParam("id") Long id, Map<String, Boolean> body,
                                 @Context ContainerRequestContext requestContext) {
        String role = (String) requestContext.getProperty("role");
        if (!"ADMIN".equals(role)) {
            throw ApiException.forbidden("Nur Administratoren können den Benutzerstatus ändern");
        }

        Boolean isActive = body.get("isActive");
        if (isActive == null) {
            throw ApiException.badRequest("isActive ist erforderlich");
        }

        userService.setUserActive(id, isActive);
        return Response.ok(Map.of("message", "Status aktualisiert")).build();
    }

    @GET
    @Path("/{id}/badges")
    @Secured
    public Response getUserBadges(@PathParam("id") Long id, @Context ContainerRequestContext requestContext) {
        Long currentUserId = (Long) requestContext.getProperty("userId");
        String role = (String) requestContext.getProperty("role");

        // Benutzer können ihre eigenen Abzeichen ansehen, Administratoren können die Abzeichen aller ansehen.
        if (!currentUserId.equals(id) && !"ADMIN".equals(role)) {
            throw ApiException.forbidden("Sie können nur Ihre eigenen Abzeichen anzeigen");
        }

        List<UserBadgeDTO> badges = gamificationService.getUserBadges(id).stream()
                .map(UserBadgeDTO::fromEntity)
                .collect(java.util.stream.Collectors.toList());
        return Response.ok(badges).build();
    }

    @GET
    @Path("/me/badges")
    @Secured
    public Response getCurrentUserBadges(@Context ContainerRequestContext requestContext) {
        Long userId = (Long) requestContext.getProperty("userId");
        List<UserBadgeDTO> badges = gamificationService.getUserBadges(userId).stream()
                .map(UserBadgeDTO::fromEntity)
                .collect(java.util.stream.Collectors.toList());
        return Response.ok(badges).build();
    }

    @GET
    @Path("/leaderboard")
    @Secured
    public Response getLeaderboard(@QueryParam("limit") @DefaultValue("10") int limit) {
        if (limit < 1 || limit > 100) {
            limit = 10;
        }
        List<UserDTO> leaderboard = userService.getLeaderboard(limit);
        return Response.ok(leaderboard).build();
    }

    @GET
    @Path("/badges")
    @Secured
    public Response getAllBadges() {
        List<BadgeDTO> badges = gamificationService.getAllBadges().stream()
                .map(BadgeDTO::fromEntity)
                .collect(java.util.stream.Collectors.toList());
        return Response.ok(badges).build();
    }
}
