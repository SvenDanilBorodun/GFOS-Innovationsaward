package com.gfos.ideaboard.resource;

import com.gfos.ideaboard.dto.SurveyDTO;
import com.gfos.ideaboard.exception.ApiException;
import com.gfos.ideaboard.security.Secured;
import com.gfos.ideaboard.service.SurveyService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Path("/surveys")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Secured
public class SurveyResource {

    @Inject
    private SurveyService surveyService;

    @GET
    public Response getSurveys(
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size,
            @Context ContainerRequestContext requestContext) {
        Long userId = (Long) requestContext.getProperty("userId");
        List<SurveyDTO> surveys = surveyService.getSurveys(page, size, userId);

        Map<String, Object> response = new HashMap<>();
        response.put("content", surveys);
        response.put("number", page);
        response.put("size", size);

        return Response.ok(response).build();
    }

    @GET
    @Path("/active")
    public Response getActiveSurveys(@Context ContainerRequestContext requestContext) {
        Long userId = (Long) requestContext.getProperty("userId");
        List<SurveyDTO> surveys = surveyService.getActiveSurveys(userId);
        return Response.ok(surveys).build();
    }

    @GET
    @Path("/{id}")
    public Response getSurvey(@PathParam("id") Long id, @Context ContainerRequestContext requestContext) {
        Long userId = (Long) requestContext.getProperty("userId");
        SurveyDTO survey = surveyService.getSurveyById(id, userId);
        return Response.ok(survey).build();
    }

    @POST
    public Response createSurvey(Map<String, Object> body, @Context ContainerRequestContext requestContext) {
        Long userId = (Long) requestContext.getProperty("userId");

        String question = (String) body.get("question");
        String description = (String) body.get("description");
        @SuppressWarnings("unchecked")
        List<String> options = (List<String>) body.get("options");
        Boolean isAnonymous = (Boolean) body.get("isAnonymous");
        Boolean allowMultipleVotes = (Boolean) body.get("allowMultipleVotes");

        if (question == null || question.trim().isEmpty()) {
            throw ApiException.badRequest("Frage ist erforderlich");
        }

        SurveyDTO survey = surveyService.createSurvey(question, description, options,
                isAnonymous, allowMultipleVotes, userId);
        return Response.status(Response.Status.CREATED).entity(survey).build();
    }

    @POST
    @Path("/{id}/vote")
    public Response vote(@PathParam("id") Long id, Map<String, Object> body,
                         @Context ContainerRequestContext requestContext) {
        Long userId = (Long) requestContext.getProperty("userId");

        @SuppressWarnings("unchecked")
        List<Number> optionIdsRaw = (List<Number>) body.get("optionIds");
        if (optionIdsRaw == null || optionIdsRaw.isEmpty()) {
            throw ApiException.badRequest("Mindestens eine Option muss ausgewählt werden");
        }

        List<Long> optionIds = optionIdsRaw.stream()
                .map(Number::longValue)
                .toList();

        SurveyDTO survey = surveyService.vote(id, optionIds, userId);
        return Response.ok(survey).build();
    }

    @DELETE
    @Path("/{id}")
    public Response deleteSurvey(@PathParam("id") Long id, @Context ContainerRequestContext requestContext) {
        Long userId = (Long) requestContext.getProperty("userId");
        surveyService.deleteSurvey(id, userId);
        return Response.noContent().build();
    }
}
