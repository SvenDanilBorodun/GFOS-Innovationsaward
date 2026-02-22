package com.gfos.ideaboard.exception;

import jakarta.ws.rs.core.Response;

public class ApiException extends RuntimeException {

    private final Response.Status status;

    public ApiException(String message, Response.Status status) {
        super(message);
        this.status = status;
    }

    public ApiException(String message) {
        this(message, Response.Status.BAD_REQUEST);
    }

    public Response.Status getStatus() {
        return status;
    }

    public static ApiException notFound(String message) {
        return new ApiException(message, Response.Status.NOT_FOUND);
    }

    public static ApiException unauthorized(String message) {
        return new ApiException(message, Response.Status.UNAUTHORIZED);
    }

    public static ApiException forbidden(String message) {
        return new ApiException(message, Response.Status.FORBIDDEN);
    }

    public static ApiException badRequest(String message) {
        return new ApiException(message, Response.Status.BAD_REQUEST);
    }

    public static ApiException conflict(String message) {
        return new ApiException(message, Response.Status.CONFLICT);
    }

    public static ApiException serverError(String message) {
        return new ApiException(message, Response.Status.INTERNAL_SERVER_ERROR);
    }
}
