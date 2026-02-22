package com.gfos.ideaboard.resource;

import com.gfos.ideaboard.dto.ChecklistItemDTO;
import com.gfos.ideaboard.dto.ChecklistToggleResponse;
import com.gfos.ideaboard.dto.CommentDTO;
import com.gfos.ideaboard.dto.FileAttachmentDTO;
import com.gfos.ideaboard.dto.IdeaDTO;
import com.gfos.ideaboard.entity.FileAttachment;
import com.gfos.ideaboard.entity.IdeaStatus;
import com.gfos.ideaboard.exception.ApiException;
import com.gfos.ideaboard.security.Secured;
import com.gfos.ideaboard.service.ChecklistService;
import com.gfos.ideaboard.service.CommentService;
import com.gfos.ideaboard.service.FileService;
import com.gfos.ideaboard.service.IdeaService;
import com.gfos.ideaboard.service.LikeService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Path("/ideas")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Secured
public class IdeaResource {

    @Inject
    private IdeaService ideaService;

    @Inject
    private LikeService likeService;

    @Inject
    private CommentService commentService;

    @Inject
    private FileService fileService;

    @Inject
    private ChecklistService checklistService;

    @GET
    public Response getIdeas(
            @QueryParam("category") String category,
            @QueryParam("status") String statusStr,
            @QueryParam("authorId") Long authorId,
            @QueryParam("search") String search,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("12") int size,
            @Context ContainerRequestContext requestContext) {

        Long userId = (Long) requestContext.getProperty("userId");
        IdeaStatus status = statusStr != null ? IdeaStatus.valueOf(statusStr) : null;

        List<IdeaDTO> ideas = ideaService.getIdeas(category, status, authorId, search, page, size, userId);
        long total = ideaService.countIdeas(category, status, authorId, search);

        Map<String, Object> response = new HashMap<>();
        response.put("content", ideas);
        response.put("totalElements", total);
        response.put("totalPages", (int) Math.ceil((double) total / size));
        response.put("size", size);
        response.put("number", page);
        response.put("first", page == 0);
        response.put("last", (page + 1) * size >= total);

        return Response.ok(response).build();
    }

    @GET
    @Path("/{id}")
    public Response getIdea(@PathParam("id") Long id, @Context ContainerRequestContext requestContext) {
        Long userId = (Long) requestContext.getProperty("userId");
        IdeaDTO idea = ideaService.getIdeaById(id, userId);
        return Response.ok(idea).build();
    }

    @POST
    public Response createIdea(Map<String, Object> body, @Context ContainerRequestContext requestContext) {
        Long userId = (Long) requestContext.getProperty("userId");

        String title = (String) body.get("title");
        String description = (String) body.get("description");
        String category = (String) body.get("category");
        @SuppressWarnings("unchecked")
        List<String> tags = (List<String>) body.get("tags");
        @SuppressWarnings("unchecked")
        List<String> checklistItems = (List<String>) body.get("checklistItems");

        if (title == null || title.trim().isEmpty()) {
            throw ApiException.badRequest("Titel ist erforderlich");
        }
        if (description == null || description.trim().isEmpty()) {
            throw ApiException.badRequest("Beschreibung ist erforderlich");
        }
        if (category == null || category.trim().isEmpty()) {
            throw ApiException.badRequest("Kategorie ist erforderlich");
        }
        if (checklistItems == null || checklistItems.isEmpty()) {
            throw ApiException.badRequest("Mindestens ein To-do ist erforderlich");
        }

        IdeaDTO idea = ideaService.createIdea(title, description, category, tags, checklistItems, userId);
        return Response.status(Response.Status.CREATED).entity(idea).build();
    }

    @PUT
    @Path("/{id}")
    public Response updateIdea(@PathParam("id") Long id, Map<String, Object> body,
                               @Context ContainerRequestContext requestContext) {
        Long userId = (Long) requestContext.getProperty("userId");

        String title = (String) body.get("title");
        String description = (String) body.get("description");
        String category = (String) body.get("category");
        @SuppressWarnings("unchecked")
        List<String> tags = (List<String>) body.get("tags");

        IdeaDTO idea = ideaService.updateIdea(id, title, description, category, tags, userId);
        return Response.ok(idea).build();
    }

    @PUT
    @Path("/{id}/status")
    public Response updateStatus(@PathParam("id") Long id, Map<String, Object> body,
                                 @Context ContainerRequestContext requestContext) {
        Long userId = (Long) requestContext.getProperty("userId");

        String statusStr = (String) body.get("status");
        Integer progressPercentage = body.get("progressPercentage") != null
                ? ((Number) body.get("progressPercentage")).intValue()
                : null;

        if (statusStr == null) {
            throw ApiException.badRequest("Status ist erforderlich");
        }

        IdeaStatus status = IdeaStatus.valueOf(statusStr);
        IdeaDTO idea = ideaService.updateStatus(id, status, progressPercentage, userId);
        return Response.ok(idea).build();
    }

    @DELETE
    @Path("/{id}")
    public Response deleteIdea(@PathParam("id") Long id, @Context ContainerRequestContext requestContext) {
        Long userId = (Long) requestContext.getProperty("userId");
        ideaService.deleteIdea(id, userId);
        return Response.noContent().build();
    }

    // Favoriten (Likes/Favorites)
    @POST
    @Path("/{id}/like")
    public Response likeIdea(@PathParam("id") Long id, @Context ContainerRequestContext requestContext) {
        Long userId = (Long) requestContext.getProperty("userId");
        likeService.likeIdea(id, userId);
        return Response.ok(Map.of("message", "Favorisiert")).build();
    }

    @DELETE
    @Path("/{id}/like")
    public Response unlikeIdea(@PathParam("id") Long id, @Context ContainerRequestContext requestContext) {
        Long userId = (Long) requestContext.getProperty("userId");
        likeService.unlikeIdea(id, userId);
        return Response.ok(Map.of("message", "Nicht mehr favorisiert")).build();
    }

    // Kommentare (Comments)
    @GET
    @Path("/{id}/comments")
    public Response getComments(@PathParam("id") Long id,
                                @Context ContainerRequestContext requestContext) {
        Long userId = (Long) requestContext.getProperty("userId");
        List<CommentDTO> comments = commentService.getCommentsByIdea(id, userId);
        return Response.ok(comments).build();
    }

    @POST
    @Path("/{id}/comments")
    public Response createComment(@PathParam("id") Long id, Map<String, String> body,
                                  @Context ContainerRequestContext requestContext) {
        Long userId = (Long) requestContext.getProperty("userId");
        String content = body.get("content");
        CommentDTO comment = commentService.createComment(id, content, userId);
        return Response.status(Response.Status.CREATED).entity(comment).build();
    }

    // Kategorien und Tags (Categories and Tags)
    @GET
    @Path("/categories")
    public Response getCategories() {
        List<String> categories = ideaService.getCategories();
        return Response.ok(categories).build();
    }

    @GET
    @Path("/tags/popular")
    public Response getPopularTags(@QueryParam("limit") @DefaultValue("20") int limit) {
        List<String> tags = ideaService.getPopularTags(limit);
        return Response.ok(tags).build();
    }

    // Dateianhänge (File Attachments)
    @POST
    @Path("/{id}/files")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response uploadFile(
            @PathParam("id") Long id,
            @FormDataParam("file") InputStream fileInputStream,
            @FormDataParam("file") FormDataContentDisposition fileDetail,
            @Context ContainerRequestContext requestContext) {
        Long userId = (Long) requestContext.getProperty("userId");

        try {
            byte[] fileData = fileInputStream.readAllBytes();
            String mimeType = determineMimeType(fileDetail.getFileName());

            FileAttachmentDTO attachment = fileService.uploadFile(
                    id, fileDetail.getFileName(), mimeType, fileData, userId);
            return Response.status(Response.Status.CREATED).entity(attachment).build();
        } catch (Exception e) {
            throw ApiException.serverError("Fehler beim Hochladen der Datei: " + e.getMessage());
        }
    }

    @GET
    @Path("/{id}/files/{fileId}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response downloadFile(
            @PathParam("id") Long id,
            @PathParam("fileId") Long fileId) {
        byte[] fileData = fileService.getFile(id, fileId);
        FileAttachment attachment = fileService.getFileAttachment(fileId);

        return Response.ok(fileData)
                .header("Content-Disposition", "attachment; filename=\"" + attachment.getOriginalName() + "\"")
                .header("Content-Type", attachment.getMimeType())
                .build();
    }

    @DELETE
    @Path("/{id}/files/{fileId}")
    public Response deleteFile(
            @PathParam("id") Long id,
            @PathParam("fileId") Long fileId,
            @Context ContainerRequestContext requestContext) {
        Long userId = (Long) requestContext.getProperty("userId");
        fileService.deleteFile(id, fileId, userId);
        return Response.noContent().build();
    }

    // Checklisten-Endpunkte (Checklist Endpoints)
    @GET
    @Path("/{id}/checklist")
    public Response getChecklist(@PathParam("id") Long id) {
        List<ChecklistItemDTO> items = checklistService.getChecklistByIdea(id);
        return Response.ok(items).build();
    }

    @POST
    @Path("/{id}/checklist")
    public Response createChecklistItem(@PathParam("id") Long id, Map<String, String> body,
                                        @Context ContainerRequestContext requestContext) {
        Long userId = (Long) requestContext.getProperty("userId");
        String title = body.get("title");

        if (title == null || title.trim().isEmpty()) {
            throw ApiException.badRequest("Titel ist erforderlich");
        }

        ChecklistItemDTO item = checklistService.createChecklistItem(id, title, userId);
        return Response.status(Response.Status.CREATED).entity(item).build();
    }

    @PATCH
    @Path("/{id}/checklist/{itemId}/toggle")
    public Response toggleChecklistItem(@PathParam("id") Long id, @PathParam("itemId") Long itemId,
                                        @Context ContainerRequestContext requestContext) {
        Long userId = (Long) requestContext.getProperty("userId");
        ChecklistToggleResponse result = checklistService.toggleChecklistItem(id, itemId, userId);
        return Response.ok(result).build();
    }

    @PUT
    @Path("/{id}/checklist/{itemId}")
    public Response updateChecklistItem(@PathParam("id") Long id, @PathParam("itemId") Long itemId,
                                        Map<String, String> body,
                                        @Context ContainerRequestContext requestContext) {
        Long userId = (Long) requestContext.getProperty("userId");
        String title = body.get("title");
        ChecklistItemDTO item = checklistService.updateChecklistItem(id, itemId, title, userId);
        return Response.ok(item).build();
    }

    @DELETE
    @Path("/{id}/checklist/{itemId}")
    public Response deleteChecklistItem(@PathParam("id") Long id, @PathParam("itemId") Long itemId,
                                        @Context ContainerRequestContext requestContext) {
        Long userId = (Long) requestContext.getProperty("userId");
        checklistService.deleteChecklistItem(id, itemId, userId);
        return Response.noContent().build();
    }

    private String determineMimeType(String filename) {
        if (filename == null) return "application/octet-stream";
        String lower = filename.toLowerCase();
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".gif")) return "image/gif";
        if (lower.endsWith(".webp")) return "image/webp";
        if (lower.endsWith(".pdf")) return "application/pdf";
        if (lower.endsWith(".doc")) return "application/msword";
        if (lower.endsWith(".docx")) return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        if (lower.endsWith(".xls")) return "application/vnd.ms-excel";
        if (lower.endsWith(".xlsx")) return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        if (lower.endsWith(".txt")) return "text/plain";
        if (lower.endsWith(".csv")) return "text/csv";
        return "application/octet-stream";
    }
}
