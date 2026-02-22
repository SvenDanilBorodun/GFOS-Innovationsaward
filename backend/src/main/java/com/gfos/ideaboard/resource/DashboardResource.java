package com.gfos.ideaboard.resource;

import com.gfos.ideaboard.dto.IdeaDTO;
import com.gfos.ideaboard.entity.IdeaStatus;
import com.gfos.ideaboard.security.Secured;
import com.gfos.ideaboard.service.IdeaService;
import com.gfos.ideaboard.service.SurveyService;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.ws.rs.*;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Path("/dashboard")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Secured
public class DashboardResource {

    @PersistenceContext(unitName = "IdeaBoardPU")
    private EntityManager em;

    @Inject
    private IdeaService ideaService;

    @Inject
    private SurveyService surveyService;

    @GET
    @Path("/statistics")
    public Response getStatistics() {
        Map<String, Object> stats = new HashMap<>();

        // Gesamtzahl der Ideen
        Long totalIdeas = em.createQuery("SELECT COUNT(i) FROM Idea i", Long.class).getSingleResult();
        stats.put("totalIdeas", totalIdeas);

        // Gesamtzahl der Benutzer
        Long totalUsers = em.createQuery("SELECT COUNT(u) FROM User u WHERE u.isActive = true", Long.class).getSingleResult();
        stats.put("totalUsers", totalUsers);

        // Ideen diese Woche - Sonntag derselben oder vorherigen Woche abrufen
        LocalDateTime weekStartTime = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY)).atStartOfDay();
        Long ideasThisWeek = em.createQuery(
                "SELECT COUNT(i) FROM Idea i WHERE i.createdAt >= :weekStart", Long.class)
                .setParameter("weekStart", weekStartTime)
                .getSingleResult();
        stats.put("ideasThisWeek", ideasThisWeek);

        // Statuszahlen - Enum-Parameter statt String-Literale verwenden
        Long conceptIdeas = em.createQuery("SELECT COUNT(i) FROM Idea i WHERE i.status = :status", Long.class)
                .setParameter("status", IdeaStatus.CONCEPT).getSingleResult();
        Long inProgressIdeas = em.createQuery("SELECT COUNT(i) FROM Idea i WHERE i.status = :status", Long.class)
                .setParameter("status", IdeaStatus.IN_PROGRESS).getSingleResult();
        Long completedIdeas = em.createQuery("SELECT COUNT(i) FROM Idea i WHERE i.status = :status", Long.class)
                .setParameter("status", IdeaStatus.COMPLETED).getSingleResult();
        stats.put("conceptIdeas", conceptIdeas);
        stats.put("inProgressIdeas", inProgressIdeas);
        stats.put("completedIdeas", completedIdeas);

        // Gesamtzahl der Likes
        Long totalLikes = em.createQuery("SELECT COUNT(l) FROM Like l", Long.class).getSingleResult();
        stats.put("totalLikes", totalLikes);

        // Gesamtzahl der Kommentare
        Long totalComments = em.createQuery("SELECT COUNT(c) FROM Comment c", Long.class).getSingleResult();
        stats.put("totalComments", totalComments);

        // Aktive Umfragen
        Long activeSurveys = em.createQuery("SELECT COUNT(s) FROM Survey s WHERE s.isActive = true", Long.class).getSingleResult();
        stats.put("activeSurveys", activeSurveys);

        // Beliebteste Kategorie
        List<Object[]> categoryResults = em.createQuery(
                "SELECT i.category, COUNT(i) as cnt FROM Idea i GROUP BY i.category ORDER BY cnt DESC", Object[].class)
                .getResultList();
        stats.put("popularCategory", categoryResults.isEmpty() ? "N/A" : categoryResults.get(0)[0]);

        // Kategorieaufschlüsselung für Diagramme
        List<Map<String, Object>> categoryBreakdown = categoryResults.stream()
                .map(row -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("category", row[0]);
                    item.put("count", row[1]);
                    return item;
                })
                .collect(Collectors.toList());
        stats.put("categoryBreakdown", categoryBreakdown);

        // Wöchentliche Aktivität (Ideen pro Tag diese Woche) - Native Abfrage für PostgreSQL-Datumstrunkierung verwenden
        // weekStartTime von oben wiederverwenden
        java.sql.Timestamp weekStartTs = java.sql.Timestamp.valueOf(weekStartTime);
        @SuppressWarnings("unchecked")
        List<Object[]> weeklyActivity = em.createNativeQuery(
                "SELECT DATE(created_at) as day, COUNT(*) as cnt FROM ideas " +
                "WHERE created_at >= ?1 GROUP BY DATE(created_at) ORDER BY day")
                .setParameter(1, weekStartTs)
                .getResultList();
        List<Map<String, Object>> activityData = weeklyActivity.stream()
                .map(row -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("date", row[0].toString());
                    item.put("ideas", ((Number) row[1]).longValue());
                    return item;
                })
                .collect(Collectors.toList());
        stats.put("weeklyActivity", activityData);

        return Response.ok(stats).build();
    }

    @GET
    @Path("/top-ideas")
    public Response getTopIdeas(@Context ContainerRequestContext requestContext) {
        Long userId = (Long) requestContext.getProperty("userId");
        List<IdeaDTO> topIdeas = ideaService.getTopIdeasThisWeek(3, userId);

        // Ranginformationen hinzufügen
        List<Map<String, Object>> result = topIdeas.stream()
                .map(idea -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("idea", idea);
                    item.put("rank", topIdeas.indexOf(idea) + 1);
                    item.put("weeklyLikes", idea.getLikeCount()); // Simplified
                    return item;
                })
                .collect(Collectors.toList());

        return Response.ok(result).build();
    }

    @GET
    @Path("/new-ideas")
    public Response getNewIdeas(
            @QueryParam("limit") @DefaultValue("5") int limit,
            @Context ContainerRequestContext requestContext) {
        Long userId = (Long) requestContext.getProperty("userId");
        List<IdeaDTO> ideas = ideaService.getIdeas(null, null, null, null, 0, limit, userId);
        return Response.ok(ideas).build();
    }

    @GET
    @Path("/surveys")
    public Response getActiveSurveys(@Context ContainerRequestContext requestContext) {
        Long userId = (Long) requestContext.getProperty("userId");
        return Response.ok(surveyService.getActiveSurveys(userId)).build();
    }
}
