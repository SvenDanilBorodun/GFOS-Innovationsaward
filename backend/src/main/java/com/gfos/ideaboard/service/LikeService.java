package com.gfos.ideaboard.service;

import com.gfos.ideaboard.entity.Idea;
import com.gfos.ideaboard.entity.Like;
import com.gfos.ideaboard.entity.User;
import com.gfos.ideaboard.exception.ApiException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;

@ApplicationScoped
public class LikeService {

    private static final int MAX_WEEKLY_LIKES = 3;
    private static final int XP_FOR_LIKE_RECEIVED = 10;

    @PersistenceContext(unitName = "IdeaBoardPU")
    private EntityManager em;

    @Inject
    private UserService userService;

    @Inject
    private NotificationService notificationService;

    @Inject
    private GamificationService gamificationService;

    public int getRemainingLikes(Long userId) {
        LocalDateTime weekStart = getLastSundayMidnight();
        Long usedLikes = em.createNamedQuery("Like.countByUserSince", Long.class)
                .setParameter("userId", userId)
                .setParameter("since", weekStart)
                .getSingleResult();
        return Math.max(0, MAX_WEEKLY_LIKES - usedLikes.intValue());
    }

    public int getWeeklyLikesUsed(Long userId) {
        LocalDateTime weekStart = getLastSundayMidnight();
        Long usedLikes = em.createNamedQuery("Like.countByUserSince", Long.class)
                .setParameter("userId", userId)
                .setParameter("since", weekStart)
                .getSingleResult();
        return usedLikes.intValue();
    }

    @Transactional
    public void likeIdea(Long ideaId, Long userId) {
        // Verbleibende Likes prüfen
        int remaining = getRemainingLikes(userId);
        if (remaining <= 0) {
            throw ApiException.badRequest("Keine Likes verbleibend diese Woche. Setzt sich jeden Sonntag um Mitternacht zurück.");
        }

        // Prüfe, ob bereits geliked
        Like existingLike = findLike(userId, ideaId);
        if (existingLike != null) {
            throw ApiException.conflict("Du magst diese Idee bereits");
        }

        User user = em.find(User.class, userId);
        Idea idea = em.find(Idea.class, ideaId);

        if (user == null || idea == null) {
            throw ApiException.notFound("Benutzer oder Idee nicht gefunden");
        }

        // Kann eigene Idee nicht liken
        if (idea.getAuthor().getId().equals(userId)) {
            throw ApiException.badRequest("Du kannst deine eigene Idee nicht liken");
        }

        // Like erstellen
        Like like = new Like();
        like.setUser(user);
        like.setIdea(idea);
        em.persist(like);

        // Hinweis: like_count wird automatisch durch Datenbank-Trigger aktualisiert
        // NICHT manuell inkrementieren, um Doppelzählung zu vermeiden

        // XP an Ideenschöpfer vergeben und Abzeichen prüfen
        gamificationService.awardXpForLikeReceived(idea.getAuthor().getId());

        // Ideenschöpfer benachrichtigen
        notificationService.notifyLike(idea, user);
    }

    @Transactional
    public void unlikeIdea(Long ideaId, Long userId) {
        Like like = findLike(userId, ideaId);
        if (like == null) {
            throw ApiException.notFound("Like nicht gefunden");
        }

        // Hinweis: like_count wird automatisch durch Datenbank-Trigger aktualisiert
        // NICHT manuell dekrementieren, um Doppelzählung zu vermeiden

        em.remove(like);
    }

    public boolean hasUserLiked(Long userId, Long ideaId) {
        return findLike(userId, ideaId) != null;
    }

    private Like findLike(Long userId, Long ideaId) {
        try {
            return em.createNamedQuery("Like.findByUserAndIdea", Like.class)
                    .setParameter("userId", userId)
                    .setParameter("ideaId", ideaId)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    private LocalDateTime getLastSundayMidnight() {
        LocalDate today = LocalDate.now();
        LocalDate lastSunday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));
        return lastSunday.atStartOfDay();
    }
}
