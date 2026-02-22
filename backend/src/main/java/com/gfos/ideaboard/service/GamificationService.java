package com.gfos.ideaboard.service;

import com.gfos.ideaboard.entity.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class GamificationService {

    @PersistenceContext(unitName = "IdeaBoardPU")
    private EntityManager em;

    @Inject
    private NotificationService notificationService;

    // XP-Belohnungen
    public static final int XP_SUBMIT_IDEA = 50;
    public static final int XP_RECEIVE_LIKE = 10;
    public static final int XP_POST_COMMENT = 5;
    public static final int XP_IDEA_COMPLETED = 100;

    // Level-Schwellenwerte
    private static final int[] LEVEL_THRESHOLDS = {0, 100, 300, 600, 1000, 1500, 2500, 4000, 6000, 10000};

    @Transactional
    public void awardXpForIdea(Long userId) {
        awardXp(userId, XP_SUBMIT_IDEA);
        checkAndAwardBadge(userId, "first_idea", this::hasSubmittedFirstIdea);
    }

    @Transactional
    public void awardXpForLikeReceived(Long ideaAuthorId) {
        awardXp(ideaAuthorId, XP_RECEIVE_LIKE);
        checkAndAwardBadge(ideaAuthorId, "popular", this::hasReceivedTenLikes);
    }

    @Transactional
    public void awardXpForComment(Long userId) {
        awardXp(userId, XP_POST_COMMENT);
        checkAndAwardBadge(userId, "commentator", this::hasPostedFiftyComments);
    }

    @Transactional
    public void awardXpForIdeaCompleted(Long ideaAuthorId) {
        awardXp(ideaAuthorId, XP_IDEA_COMPLETED);
    }

    @Transactional
    public void awardXp(Long userId, int xpAmount) {
        User user = em.find(User.class, userId);
        if (user == null) return;

        int oldLevel = user.getLevel();
        int newXp = user.getXpPoints() + xpAmount;
        user.setXpPoints(newXp);

        int newLevel = calculateLevel(newXp);
        if (newLevel > oldLevel) {
            user.setLevel(newLevel);
            notificationService.createNotification(
                    userId,
                    NotificationType.BADGE_EARNED,
                    "Herzlichen Glückwunsch! Du hast Level " + newLevel + " erreicht!",
                    null, null
            );
        }

        em.merge(user);
    }

    public int calculateLevel(int xp) {
        for (int i = LEVEL_THRESHOLDS.length - 1; i >= 0; i--) {
            if (xp >= LEVEL_THRESHOLDS[i]) {
                return i + 1;
            }
        }
        return 1;
    }

    public int getXpForNextLevel(int currentLevel) {
        if (currentLevel >= LEVEL_THRESHOLDS.length) {
            return -1; // Max level reached
        }
        return LEVEL_THRESHOLDS[currentLevel];
    }

    public int getXpProgress(User user) {
        int currentLevelXp = user.getLevel() > 1 ? LEVEL_THRESHOLDS[user.getLevel() - 1] : 0;
        int nextLevelXp = getXpForNextLevel(user.getLevel());
        if (nextLevelXp == -1) return 100;

        int xpInCurrentLevel = user.getXpPoints() - currentLevelXp;
        int xpNeededForLevel = nextLevelXp - currentLevelXp;
        return (int) ((double) xpInCurrentLevel / xpNeededForLevel * 100);
    }

    @Transactional
    public void checkAndAwardBadge(Long userId, String badgeName, BadgeChecker checker) {
        // Prüfen, ob der Benutzer dieses Abzeichen bereits hat
        List<UserBadge> existing = em.createQuery(
                "SELECT ub FROM UserBadge ub WHERE ub.user.id = :userId AND ub.badge.name = :badgeName",
                UserBadge.class)
                .setParameter("userId", userId)
                .setParameter("badgeName", badgeName)
                .getResultList();

        if (!existing.isEmpty()) return;

        // Prüfen, ob Kriterien erfüllt sind
        if (!checker.check(userId)) return;

        // Abzeichen finden
        List<Badge> badges = em.createQuery("SELECT b FROM Badge b WHERE b.name = :name", Badge.class)
                .setParameter("name", badgeName)
                .getResultList();

        if (badges.isEmpty()) return;

        Badge badge = badges.get(0);

        // Abzeichen verleihen
        User user = em.find(User.class, userId);
        UserBadge userBadge = new UserBadge();
        userBadge.setUser(user);
        userBadge.setBadge(badge);
        em.persist(userBadge);

        // Benutzer benachrichtigen
        notificationService.createNotification(
                userId,
                NotificationType.BADGE_EARNED,
                "Du hast das Abzeichen \"" + badge.getDisplayName() + "\" erhalten!",
                null, null
        );
    }

    // Prüfer für Abzeichenkriterien
    private boolean hasSubmittedFirstIdea(Long userId) {
        Long count = em.createQuery("SELECT COUNT(i) FROM Idea i WHERE i.author.id = :userId", Long.class)
                .setParameter("userId", userId)
                .getSingleResult();
        return count >= 1;
    }

    private boolean hasReceivedTenLikes(Long userId) {
        Long count = em.createQuery(
                "SELECT SUM(i.likeCount) FROM Idea i WHERE i.author.id = :userId", Long.class)
                .setParameter("userId", userId)
                .getSingleResult();
        return count != null && count >= 10;
    }

    private boolean hasPostedFiftyComments(Long userId) {
        Long count = em.createQuery("SELECT COUNT(c) FROM Comment c WHERE c.author.id = :userId", Long.class)
                .setParameter("userId", userId)
                .getSingleResult();
        return count >= 50;
    }

    public List<Badge> getAllBadges() {
        return em.createQuery("SELECT b FROM Badge b ORDER BY b.id", Badge.class)
                .getResultList();
    }

    public List<UserBadge> getUserBadges(Long userId) {
        return em.createQuery(
                "SELECT ub FROM UserBadge ub WHERE ub.user.id = :userId ORDER BY ub.earnedAt DESC",
                UserBadge.class)
                .setParameter("userId", userId)
                .getResultList();
    }

    @FunctionalInterface
    public interface BadgeChecker {
        boolean check(Long userId);
    }
}
