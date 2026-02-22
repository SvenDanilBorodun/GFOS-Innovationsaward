package com.gfos.ideaboard.exception;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Provider
public class GlobalExceptionHandler implements ExceptionMapper<Exception> {

    @Override
    public Response toResponse(Exception exception) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", LocalDateTime.now().toString());

        if (exception instanceof ApiException apiException) {
            errorResponse.put("status", apiException.getStatus().getStatusCode());
            errorResponse.put("error", apiException.getStatus().getReasonPhrase());
            errorResponse.put("message", apiException.getMessage());

            return Response.status(apiException.getStatus())
                    .entity(errorResponse)
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }

        // Unerwartete Ausnahmen protokollieren
        exception.printStackTrace();

        errorResponse.put("status", Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
        errorResponse.put("error", "Interner Serverfehler");
        errorResponse.put("message", "Ein unerwarteter Fehler ist aufgetreten");

        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(errorResponse)
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
