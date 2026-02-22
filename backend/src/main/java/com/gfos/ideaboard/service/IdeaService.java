package com.gfos.ideaboard.service;

import com.gfos.ideaboard.dto.IdeaDTO;
import com.gfos.ideaboard.entity.*;
import com.gfos.ideaboard.exception.ApiException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.transaction.Transactional;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class IdeaService {

    private static final int XP_FOR_IDEA = 50;
    private static final int XP_FOR_COMPLETED = 100;

    @PersistenceContext(unitName = "IdeaBoardPU")
    private EntityManager em;

    @Inject
    private UserService userService;

    @Inject
    private AuditService auditService;

    @Inject
    private NotificationService notificationService;

    @Inject
    private GamificationService gamificationService;

    @Inject
    private GroupService groupService;

    public Idea findById(Long id) {
        return em.find(Idea.class, id);
    }

    @Transactional
    public IdeaDTO getIdeaById(Long id, Long currentUserId) {
        Idea idea = findById(id);
        if (idea == null) {
            throw ApiException.notFound("Idee nicht gefunden");
        }

        // Aufrufe erhöhen
        incrementViewCount(id);

        boolean isLiked = isLikedByUser(id, currentUserId);
        return IdeaDTO.fromEntity(idea, isLiked);
    }

    public List<IdeaDTO> getIdeas(String category, IdeaStatus status, Long authorId,
                                   String search, int page, int size, Long currentUserId) {
        StringBuilder jpql = new StringBuilder("SELECT i FROM Idea i WHERE 1=1");

        if (category != null && !category.isEmpty()) {
            jpql.append(" AND i.category = :category");
        }
        if (status != null) {
            jpql.append(" AND i.status = :status");
        }
        if (authorId != null) {
            jpql.append(" AND i.author.id = :authorId");
        }
        if (search != null && !search.isEmpty()) {
            jpql.append(" AND (LOWER(i.title) LIKE :search OR LOWER(i.description) LIKE :search)");
        }
        jpql.append(" ORDER BY i.createdAt DESC");

        TypedQuery<Idea> query = em.createQuery(jpql.toString(), Idea.class);

        if (category != null && !category.isEmpty()) {
            query.setParameter("category", category);
        }
        if (status != null) {
            query.setParameter("status", status);
        }
        if (authorId != null) {
            query.setParameter("authorId", authorId);
        }
        if (search != null && !search.isEmpty()) {
            query.setParameter("search", "%" + search.toLowerCase() + "%");
        }

        query.setFirstResult(page * size);
        query.setMaxResults(size);

        List<Idea> ideas = query.getResultList();

        return ideas.stream()
                .map(idea -> IdeaDTO.fromEntity(idea, isLikedByUser(idea.getId(), currentUserId)))
                .collect(Collectors.toList());
    }

    public long countIdeas(String category, IdeaStatus status, Long authorId, String search) {
        StringBuilder jpql = new StringBuilder("SELECT COUNT(i) FROM Idea i WHERE 1=1");

        if (category != null && !category.isEmpty()) {
            jpql.append(" AND i.category = :category");
        }
        if (status != null) {
            jpql.append(" AND i.status = :status");
        }
        if (authorId != null) {
            jpql.append(" AND i.author.id = :authorId");
        }
        if (search != null && !search.isEmpty()) {
            jpql.append(" AND (LOWER(i.title) LIKE :search OR LOWER(i.description) LIKE :search)");
        }

        TypedQuery<Long> query = em.createQuery(jpql.toString(), Long.class);

        if (category != null && !category.isEmpty()) {
            query.setParameter("category", category);
        }
        if (status != null) {
            query.setParameter("status", status);
        }
        if (authorId != null) {
            query.setParameter("authorId", authorId);
        }
        if (search != null && !search.isEmpty()) {
            query.setParameter("search", "%" + search.toLowerCase() + "%");
        }

        return query.getSingleResult();
    }

    @Transactional
    public IdeaDTO createIdea(String title, String description, String category,
                              List<String> tags, Long authorId) {
        User author = em.find(User.class, authorId);
        if (author == null) {
            throw ApiException.notFound("Autor nicht gefunden");
        }

        Idea idea = new Idea();
        idea.setTitle(title);
        idea.setDescription(description);
        idea.setCategory(category);
        idea.setTags(tags != null ? tags : List.of());
        idea.setAuthor(author);
        idea.setStatus(IdeaStatus.CONCEPT);
        idea.setProgressPercentage(0);

        em.persist(idea);

        // Erstelle automatisch eine Gruppe für diese Idee
        groupService.createGroupForIdea(idea, author);

        // Vergebe XP und prüfe Abzeichen
        gamificationService.awardXpForIdea(authorId);

        // Audit-Protokoll
        auditService.log(authorId, AuditAction.CREATE, "Idea", idea.getId(), null, null);

        return IdeaDTO.fromEntity(idea);
    }

    @Transactional
    public IdeaDTO createIdea(String title, String description, String category,
                              List<String> tags, List<String> checklistItemTitles, Long authorId) {
        User author = em.find(User.class, authorId);
        if (author == null) {
            throw ApiException.notFound("Autor nicht gefunden");
        }

        // Validiere, dass mindestens ein Checklistenelement vorhanden ist
        if (checklistItemTitles == null || checklistItemTitles.isEmpty()) {
            throw ApiException.badRequest("Mindestens ein To-do ist erforderlich");
        }

        // Filtere leere Einträge
        List<String> validChecklistItems = checklistItemTitles.stream()
                .filter(item -> item != null && !item.trim().isEmpty())
                .map(String::trim)
                .toList();

        if (validChecklistItems.isEmpty()) {
            throw ApiException.badRequest("Mindestens ein gültiges To-do ist erforderlich");
        }

        Idea idea = new Idea();
        idea.setTitle(title);
        idea.setDescription(description);
        idea.setCategory(category);
        idea.setTags(tags != null ? tags : List.of());
        idea.setAuthor(author);
        idea.setStatus(IdeaStatus.CONCEPT);
        idea.setProgressPercentage(0);

        em.persist(idea);

        // Erstelle Checklistenelemente
        int position = 0;
        for (String itemTitle : validChecklistItems) {
            ChecklistItem item = new ChecklistItem();
            item.setIdea(idea);
            item.setTitle(itemTitle);
            item.setIsCompleted(false);
            item.setOrdinalPosition(position++);
            em.persist(item);
        }

        // Erstelle automatisch eine Gruppe für diese Idee
        groupService.createGroupForIdea(idea, author);

        // Vergebe XP und prüfe Abzeichen
        gamificationService.awardXpForIdea(authorId);

        // Audit-Protokoll
        auditService.log(authorId, AuditAction.CREATE, "Idea", idea.getId(), null, null);

        return IdeaDTO.fromEntity(idea);
    }

    @Transactional
    public IdeaDTO updateIdea(Long id, String title, String description, String category,
                              List<String> tags, Long currentUserId) {
        Idea idea = findById(id);
        if (idea == null) {
            throw ApiException.notFound("Idee nicht gefunden");
        }

        // Prüfe Eigentumsrecht (nur Autor oder Admin können aktualisieren)
        if (!idea.getAuthor().getId().equals(currentUserId)) {
            User currentUser = em.find(User.class, currentUserId);
            if (currentUser == null || currentUser.getRole() != UserRole.ADMIN) {
                throw ApiException.forbidden("Nicht berechtigt, diese Idee zu aktualisieren");
            }
        }

        if (title != null) idea.setTitle(title);
        if (description != null) idea.setDescription(description);
        if (category != null) idea.setCategory(category);
        if (tags != null) idea.setTags(tags);

        em.merge(idea);

        auditService.log(currentUserId, AuditAction.UPDATE, "Idea", id, null, null);

        return IdeaDTO.fromEntity(idea);
    }

    @Transactional
    public IdeaDTO updateStatus(Long id, IdeaStatus status, Integer progressPercentage, Long currentUserId) {
        Idea idea = findById(id);
        if (idea == null) {
            throw ApiException.notFound("Idee nicht gefunden");
        }

        User currentUser = em.find(User.class, currentUserId);
        if (currentUser == null) {
            throw ApiException.forbidden("Benutzer nicht gefunden");
        }

        boolean isAuthor = idea.getAuthor().getId().equals(currentUserId);
        boolean isPMOrAdmin = currentUser.getRole() == UserRole.PROJECT_MANAGER ||
                              currentUser.getRole() == UserRole.ADMIN;

        // Prüfe Berechtigung (Autor kann eigene Idee ändern, PM/Admin alle)
        if (!isAuthor && !isPMOrAdmin) {
            throw ApiException.forbidden("Nicht berechtigt, Status zu ändern");
        }

        // Nur PM/Admin können abgeschlossene Ideen wieder öffnen
        if (idea.getStatus() == IdeaStatus.COMPLETED && status != IdeaStatus.COMPLETED) {
            if (!isPMOrAdmin) {
                throw ApiException.forbidden("Abgeschlossene Ideen können nur von PM/Admin erneut geöffnet werden");
            }
        }

        IdeaStatus oldStatus = idea.getStatus();
        idea.setStatus(status);

        // Fortschritt kann nur vom Ideenschöpfer über die Checkliste gesetzt werden
        // PM/Admin können nur Status ändern; Fortschritt wird automatisch aus der Checkliste aktualisiert
        // Ausnahme: Wenn Status zu COMPLETED oder CONCEPT wechselt, Fortschritt automatisch setzen
        if (status == IdeaStatus.COMPLETED) {
            idea.setProgressPercentage(100);
            // Vergebe XP an den Autor für Abschluss
            gamificationService.awardXpForIdeaCompleted(idea.getAuthor().getId());
        } else if (status == IdeaStatus.CONCEPT) {
            idea.setProgressPercentage(0);
        }
        // Hinweis: Für IN_PROGRESS wird der Fortschritt nur vom Checklisten-System verwaltet

        em.merge(idea);

        // Benachrichtige den Autor über die Statusänderung
        if (oldStatus != status) {
            notificationService.notifyStatusChange(idea, oldStatus, status, currentUser);
            auditService.log(currentUserId, AuditAction.STATUS_CHANGE, "Idea", id,
                    "{\"status\":\"" + oldStatus + "\"}",
                    "{\"status\":\"" + status + "\"}");
        }

        return IdeaDTO.fromEntity(idea);
    }

    @Transactional
    public void deleteIdea(Long id, Long currentUserId) {
        Idea idea = findById(id);
        if (idea == null) {
            throw ApiException.notFound("Idee nicht gefunden");
        }

        // Nur Admin kann löschen
        User currentUser = em.find(User.class, currentUserId);
        if (currentUser == null || currentUser.getRole() != UserRole.ADMIN) {
            throw ApiException.forbidden("Nur Administratoren können Ideen löschen");
        }

        auditService.log(currentUserId, AuditAction.DELETE, "Idea", id, null, null);
        em.remove(idea);
    }

    @Transactional
    public void incrementViewCount(Long ideaId) {
        em.createQuery("UPDATE Idea i SET i.viewCount = i.viewCount + 1 WHERE i.id = :id")
                .setParameter("id", ideaId)
                .executeUpdate();
    }

    public List<IdeaDTO> getTopIdeasThisWeek(int limit, Long currentUserId) {
        // Abrufen von Ideen mit den meisten Likes (vereinfacht - sortiert nach Gesamtlikes)
        List<Idea> ideas = em.createQuery(
                "SELECT i FROM Idea i ORDER BY i.likeCount DESC, i.createdAt DESC", Idea.class)
                .setMaxResults(limit)
                .getResultList();

        return ideas.stream()
                .map(idea -> {
                    return IdeaDTO.fromEntity(idea, isLikedByUser(idea.getId(), currentUserId));
                })
                .collect(Collectors.toList());
    }

    public List<String> getCategories() {
        return em.createQuery(
                "SELECT DISTINCT i.category FROM Idea i ORDER BY i.category", String.class)
                .getResultList();
    }

    public List<String> getPopularTags(int limit) {
        return em.createQuery(
                "SELECT t FROM Idea i JOIN i.tags t GROUP BY t ORDER BY COUNT(t) DESC", String.class)
                .setMaxResults(limit)
                .getResultList();
    }

    private boolean isLikedByUser(Long ideaId, Long userId) {
        if (userId == null) return false;
        Long count = em.createQuery(
                "SELECT COUNT(l) FROM Like l WHERE l.idea.id = :ideaId AND l.user.id = :userId", Long.class)
                .setParameter("ideaId", ideaId)
                .setParameter("userId", userId)
                .getSingleResult();
        return count > 0;
    }

    private LocalDateTime getLastSundayMidnight() {
        LocalDate today = LocalDate.now();
        LocalDate lastSunday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));
        return lastSunday.atStartOfDay();
    }
}
