package com.gfos.ideaboard.service;

import com.gfos.ideaboard.dto.NotificationDTO;
import com.gfos.ideaboard.entity.Badge;
import com.gfos.ideaboard.entity.Comment;
import com.gfos.ideaboard.entity.GroupMember;
import com.gfos.ideaboard.entity.Idea;
import com.gfos.ideaboard.entity.IdeaGroup;
import com.gfos.ideaboard.entity.IdeaStatus;
import com.gfos.ideaboard.entity.Notification;
import com.gfos.ideaboard.entity.NotificationType;
import com.gfos.ideaboard.entity.User;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class NotificationService {

    @PersistenceContext(unitName = "IdeaBoardPU")
    private EntityManager em;

    public List<NotificationDTO> getNotificationsByUser(Long userId, int limit) {
        List<Notification> notifications = em.createNamedQuery("Notification.findByUser", Notification.class)
                .setParameter("userId", userId)
                .setMaxResults(limit)
                .getResultList();
        return notifications.stream()
                .map(NotificationDTO::fromEntity)
                .collect(Collectors.toList());
    }

    public long getUnreadCount(Long userId) {
        return em.createNamedQuery("Notification.countUnreadByUser", Long.class)
                .setParameter("userId", userId)
                .getSingleResult();
    }

    @Transactional
    public void markAsRead(Long notificationId, Long userId) {
        Notification notification = em.find(Notification.class, notificationId);
        if (notification != null && notification.getUser().getId().equals(userId)) {
            notification.setIsRead(true);
            em.merge(notification);
        }
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        em.createQuery("UPDATE Notification n SET n.isRead = true WHERE n.user.id = :userId AND n.isRead = false")
                .setParameter("userId", userId)
                .executeUpdate();
    }

    @Transactional
    public void notifyLike(Idea idea, User liker) {
        Notification notification = new Notification();
        notification.setUser(idea.getAuthor());
        notification.setType(NotificationType.LIKE);
        notification.setTitle("Neues Like");
        notification.setMessage(liker.getFirstName() + " hat deine Idee \"" + truncate(idea.getTitle(), 50) + "\" geliked");
        notification.setLink("/ideas/" + idea.getId());
        notification.setSender(liker);
        notification.setRelatedEntityType("Idea");
        notification.setRelatedEntityId(idea.getId());
        em.persist(notification);
    }

    @Transactional
    public void notifyComment(Idea idea, User commenter, String commentContent) {
        Notification notification = new Notification();
        notification.setUser(idea.getAuthor());
        notification.setType(NotificationType.COMMENT);
        notification.setTitle("Neuer Kommentar");
        notification.setMessage(commenter.getFirstName() + " hat \"" + truncate(idea.getTitle(), 30) + "\" kommentiert: " + truncate(commentContent, 50));
        notification.setLink("/ideas/" + idea.getId());
        notification.setSender(commenter);
        notification.setRelatedEntityType("Idea");
        notification.setRelatedEntityId(idea.getId());
        em.persist(notification);
    }

    @Transactional
    public void notifyReaction(Comment comment, User reactor, String emoji) {
        Notification notification = new Notification();
        notification.setUser(comment.getAuthor());
        notification.setType(NotificationType.REACTION);
        notification.setTitle("Neue Reaktion");
        notification.setMessage(reactor.getFirstName() + " hat auf deinen Kommentar reagiert");
        notification.setLink("/ideas/" + comment.getIdea().getId());
        notification.setSender(reactor);
        notification.setRelatedEntityType("Comment");
        notification.setRelatedEntityId(comment.getId());
        em.persist(notification);
    }

    @Transactional
    public void notifyStatusChange(Idea idea, IdeaStatus oldStatus, IdeaStatus newStatus, User changedBy) {
        Notification notification = new Notification();
        notification.setUser(idea.getAuthor());
        notification.setType(NotificationType.STATUS_CHANGE);
        notification.setTitle("Status aktualisiert");
        notification.setMessage("Der Status deiner Idee \"" + truncate(idea.getTitle(), 30) + "\" hat sich geändert auf " + formatStatus(newStatus));
        notification.setLink("/ideas/" + idea.getId());
        notification.setSender(changedBy);
        notification.setRelatedEntityType("Idea");
        notification.setRelatedEntityId(idea.getId());
        em.persist(notification);
    }

    @Transactional
    public void notifyBadgeEarned(User user, Badge badge) {
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setType(NotificationType.BADGE_EARNED);
        notification.setTitle("Abzeichen erhalten!");
        notification.setMessage("Herzlichen Glückwunsch! Du hast das Abzeichen \"" + badge.getName() + "\" erhalten");
        notification.setLink("/profile");
        notification.setRelatedEntityType("Badge");
        notification.setRelatedEntityId(badge.getId());
        em.persist(notification);
    }

    @Transactional
    public void notifyLevelUp(User user, int newLevel) {
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setType(NotificationType.LEVEL_UP);
        notification.setTitle("Level Up!");
        notification.setMessage("Herzlichen Glückwunsch! Du hast Level " + newLevel + " erreicht");
        notification.setLink("/profile");
        em.persist(notification);
    }

    @Transactional
    public void createNotification(Long userId, NotificationType type, String message,
                                    String relatedEntityType, Long relatedEntityId) {
        User user = em.find(User.class, userId);
        if (user == null) return;

        Notification notification = new Notification();
        notification.setUser(user);
        notification.setType(type);
        String gerTitle = switch (type) {
            case LIKE -> "Neues Like";
            case COMMENT -> "Neuer Kommentar";
            case REACTION -> "Neue Reaktion";
            case STATUS_CHANGE -> "Status aktualisiert";
            case BADGE_EARNED -> "Abzeichen erhalten!";
            case LEVEL_UP -> "Level Up!";
            case MENTION -> "Erwähnung";
            case MESSAGE -> "Neue Nachricht";
        };
        notification.setTitle(gerTitle);
        notification.setMessage(message);
        notification.setRelatedEntityType(relatedEntityType);
        notification.setRelatedEntityId(relatedEntityId);
        if (type == NotificationType.BADGE_EARNED || type == NotificationType.LEVEL_UP) {
            notification.setLink("/profile");
        }
        em.persist(notification);
    }

    @Transactional
    public void notifyGroupJoin(IdeaGroup group, User joiner) {
        // Den Gruppenersteller benachrichtigen
        Notification notification = new Notification();
        notification.setUser(group.getCreatedBy());
        notification.setType(NotificationType.MESSAGE);
        notification.setTitle("Neues Gruppenmitglied");
        notification.setMessage(joiner.getFirstName() + " " + joiner.getLastName() + " ist der Gruppe \"" + truncate(group.getName(), 30) + "\" beigetreten");
        notification.setLink("/messages?group=" + group.getId());
        notification.setSender(joiner);
        notification.setRelatedEntityType("IdeaGroup");
        notification.setRelatedEntityId(group.getId());
        em.persist(notification);
    }

    @Transactional
    public void notifyGroupMessage(IdeaGroup group, User sender, String content) {
        // Mitglieder mit eifrig geladenen Benutzern abfragen, um Lazy-Loading-Probleme zu vermeiden
        List<GroupMember> members = em.createNamedQuery("GroupMember.findByGroupWithUser", GroupMember.class)
                .setParameter("groupId", group.getId())
                .getResultList();

        // Absendernamen sicher abrufen
        String senderName = sender.getFirstName() != null ? sender.getFirstName() : sender.getUsername();
        String groupName = group.getName() != null ? group.getName() : "Group";

        // Alle Gruppenmitglieder außer dem Absender benachrichtigen
        for (GroupMember member : members) {
            User memberUser = member.getUser();
            if (memberUser != null && !memberUser.getId().equals(sender.getId())) {
                Notification notification = new Notification();
                notification.setUser(memberUser);
                notification.setType(NotificationType.MESSAGE);
                notification.setTitle("Neue Gruppennachricht");
                notification.setMessage(senderName + " in \"" + truncate(groupName, 20) + "\": " + truncate(content, 50));
                notification.setLink("/messages?group=" + group.getId());
                notification.setSender(sender);
                notification.setRelatedEntityType("IdeaGroup");
                notification.setRelatedEntityId(group.getId());
                em.persist(notification);
            }
        }
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength - 3) + "...";
    }

    private String formatStatus(IdeaStatus status) {
        return switch (status) {
            case CONCEPT -> "Konzept";
            case IN_PROGRESS -> "In Bearbeitung";
            case COMPLETED -> "Abgeschlossen";
        };
    }
}
