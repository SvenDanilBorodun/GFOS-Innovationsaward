package com.gfos.ideaboard.resource;

import com.gfos.ideaboard.dto.AuthRequest;
import com.gfos.ideaboard.dto.AuthResponse;
import com.gfos.ideaboard.dto.RegisterRequest;
import com.gfos.ideaboard.service.AuthService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Map;

@Path("/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthResource {

    @Inject
    private AuthService authService;

    @POST
    @Path("/login")
    public Response login(@Valid AuthRequest request) {
        AuthResponse response = authService.login(request);
        return Response.ok(response).build();
    }

    @POST
    @Path("/register")
    public Response register(@Valid RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return Response.status(Response.Status.CREATED).entity(response).build();
    }

    @POST
    @Path("/refresh")
    public Response refreshToken(Map<String, String> body) {
        String refreshToken = body.get("refreshToken");
        if (refreshToken == null || refreshToken.isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Aktualisierungstoken ist erforderlich"))
                    .build();
        }
        AuthResponse response = authService.refreshToken(refreshToken);
        return Response.ok(response).build();
    }

    @POST
    @Path("/logout")
    public Response logout() {
        // JWT ist statuslos, daher wird die Abmeldung auf der Client-Seite behandelt.
        // Dieser Endpunkt existiert für eine zukünftige Token-Blacklist-Implementierung.
        return Response.ok(Map.of("message", "Erfolgreich abgemeldet")).build();
    }
}
