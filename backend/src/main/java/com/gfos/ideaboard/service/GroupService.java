package com.gfos.ideaboard.service;

import com.gfos.ideaboard.dto.GroupMessageDTO;
import com.gfos.ideaboard.dto.IdeaGroupDTO;
import com.gfos.ideaboard.entity.*;
import com.gfos.ideaboard.exception.ApiException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class GroupService {

    private static final int MAX_MESSAGE_LENGTH = 2000;

    @PersistenceContext(unitName = "IdeaBoardPU")
    private EntityManager em;

    @Inject
    private NotificationService notificationService;

    /**
     * Erstellt eine Gruppe für eine Idee. Wird automatisch aufgerufen, wenn eine Idee erstellt wird.
     */
    @Transactional
    public IdeaGroup createGroupForIdea(Idea idea, User creator) {
        IdeaGroup group = new IdeaGroup();
        group.setIdea(idea);
        group.setName("Group: " + idea.getTitle());
        group.setDescription("Discussion group for idea: " + idea.getTitle());
        group.setCreatedBy(creator);

        em.persist(group);

        // Den Ersteller als Mitglied mit der Rolle ERSTELLER hinzufügen
        GroupMember creatorMember = new GroupMember();
        creatorMember.setGroup(group);
        creatorMember.setUser(creator);
        creatorMember.setRole(GroupMemberRole.CREATOR);

        em.persist(creatorMember);

        return group;
    }

    /**
     * Ruft alle Gruppen ab, in denen ein Benutzer Mitglied ist.
     */
    public List<IdeaGroupDTO> getUserGroups(Long userId) {
        List<IdeaGroup> groups = em.createNamedQuery("IdeaGroup.findByUser", IdeaGroup.class)
                .setParameter("userId", userId)
                .getResultList();

        return groups.stream()
                .map(group -> {
                    // Mitglieder eifrig für DTO-Konvertierung abrufen
                    List<GroupMember> members = em.createNamedQuery("GroupMember.findByGroupWithUser", GroupMember.class)
                            .setParameter("groupId", group.getId())
                            .getResultList();
                    int unreadCount = getUnreadMessageCount(group.getId(), userId);
                    GroupMessageDTO lastMessage = getLastMessage(group.getId());
                    return IdeaGroupDTO.fromEntity(group, members, unreadCount, lastMessage);
                })
                .collect(Collectors.toList());
    }

    /**
     * Ruft eine Gruppe anhand ihrer ID ab.
     */
    public IdeaGroupDTO getGroup(Long groupId, Long userId) {
        // JPQL mit JOIN FETCH verwenden, um verwandte Entitäten eifrig zu laden
        List<IdeaGroup> groups = em.createQuery(
                "SELECT g FROM IdeaGroup g LEFT JOIN FETCH g.idea LEFT JOIN FETCH g.createdBy WHERE g.id = :groupId",
                IdeaGroup.class)
                .setParameter("groupId", groupId)
                .getResultList();

        if (groups.isEmpty()) {
            throw ApiException.notFound("Group not found");
        }
        IdeaGroup group = groups.get(0);

        // Prüfen, ob Benutzer Mitglied ist
        if (!isMember(groupId, userId)) {
            throw ApiException.forbidden("You are not a member of this group");
        }

        // Mitglieder eifrig für DTO-Konvertierung abrufen
        List<GroupMember> members = em.createNamedQuery("GroupMember.findByGroupWithUser", GroupMember.class)
                .setParameter("groupId", groupId)
                .getResultList();

        int unreadCount = getUnreadMessageCount(groupId, userId);
        GroupMessageDTO lastMessage = getLastMessage(groupId);
        return IdeaGroupDTO.fromEntity(group, members, unreadCount, lastMessage);
    }

    /**
     * Ruft eine Gruppe anhand der Ideen-ID ab.
     */
    public IdeaGroupDTO getGroupByIdea(Long ideaId, Long userId) {
        List<IdeaGroup> groups = em.createNamedQuery("IdeaGroup.findByIdea", IdeaGroup.class)
                .setParameter("ideaId", ideaId)
                .getResultList();

        if (groups.isEmpty()) {
            throw ApiException.notFound("No group found for this idea");
        }

        IdeaGroup group = groups.get(0);
        boolean userIsMember = isMember(group.getId(), userId);

        // Mitglieder eifrig für DTO-Konvertierung abrufen
        List<GroupMember> members = em.createNamedQuery("GroupMember.findByGroupWithUser", GroupMember.class)
                .setParameter("groupId", group.getId())
                .getResultList();

        int unreadCount = userIsMember ? getUnreadMessageCount(group.getId(), userId) : 0;
        GroupMessageDTO lastMessage = getLastMessage(group.getId());

        return IdeaGroupDTO.fromEntity(group, members, unreadCount, lastMessage);
    }

    /**
     * Ermöglicht einem Benutzer, einer Gruppe beizutreten.
     */
    @Transactional
    public IdeaGroupDTO joinGroup(Long groupId, Long userId) {
        // JPQL mit JOIN FETCH verwenden, um verwandte Entitäten eifrig zu laden
        List<IdeaGroup> groups = em.createQuery(
                "SELECT g FROM IdeaGroup g LEFT JOIN FETCH g.idea LEFT JOIN FETCH g.createdBy WHERE g.id = :groupId",
                IdeaGroup.class)
                .setParameter("groupId", groupId)
                .getResultList();

        if (groups.isEmpty()) {
            throw ApiException.notFound("Group not found");
        }
        IdeaGroup group = groups.get(0);

        // Prüfen, ob bereits Mitglied
        if (isMember(groupId, userId)) {
            throw ApiException.badRequest("You are already a member of this group");
        }

        User user = em.find(User.class, userId);
        if (user == null) {
            throw ApiException.notFound("User not found");
        }

        GroupMember member = new GroupMember();
        member.setGroup(group);
        member.setUser(user);
        member.setRole(GroupMemberRole.MEMBER);

        em.persist(member);
        em.flush(); // Sicherstellen, dass das Mitglied vor dem Abrufen persistiert wird

        // Gruppenersteller benachrichtigen, dass jemand beigetreten ist
        notificationService.notifyGroupJoin(group, user);

        // Alle Mitglieder einschließlich des neuen für DTO-Konvertierung abrufen
        List<GroupMember> members = em.createNamedQuery("GroupMember.findByGroupWithUser", GroupMember.class)
                .setParameter("groupId", groupId)
                .getResultList();

        return IdeaGroupDTO.fromEntity(group, members, 0, null);
    }

    /**
     * Ermöglicht einem Benutzer, einer Gruppe anhand der Ideen-ID beizutreten.
     */
    @Transactional
    public IdeaGroupDTO joinGroupByIdea(Long ideaId, Long userId) {
        List<IdeaGroup> groups = em.createNamedQuery("IdeaGroup.findByIdea", IdeaGroup.class)
                .setParameter("ideaId", ideaId)
                .getResultList();

        if (groups.isEmpty()) {
            throw ApiException.notFound("No group found for this idea");
        }

        return joinGroup(groups.get(0).getId(), userId);
    }

    /**
     * Ermöglicht einem Benutzer, eine Gruppe zu verlassen.
     */
    @Transactional
    public void leaveGroup(Long groupId, Long userId) {
        IdeaGroup group = em.find(IdeaGroup.class, groupId);
        if (group == null) {
            throw ApiException.notFound("Group not found");
        }

        // Prüfen, ob der Benutzer der Ersteller ist
        if (group.getCreatedBy().getId().equals(userId)) {
            throw ApiException.badRequest("Group creator cannot leave the group");
        }

        List<GroupMember> members = em.createNamedQuery("GroupMember.findByGroupAndUser", GroupMember.class)
                .setParameter("groupId", groupId)
                .setParameter("userId", userId)
                .getResultList();

        if (members.isEmpty()) {
            throw ApiException.badRequest("You are not a member of this group");
        }

        em.remove(members.get(0));
    }

    /**
     * Ruft alle Nachrichten in einer Gruppe ab.
     */
    public List<GroupMessageDTO> getGroupMessages(Long groupId, Long userId) {
        // Verifizieren, dass Benutzer Mitglied ist
        if (!isMember(groupId, userId)) {
            throw ApiException.forbidden("You are not a member of this group");
        }

        List<GroupMessage> messages = em.createNamedQuery("GroupMessage.findByGroup", GroupMessage.class)
                .setParameter("groupId", groupId)
                .getResultList();

        return messages.stream()
                .map(GroupMessageDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Sendet eine Nachricht an eine Gruppe.
     */
    @Transactional
    public GroupMessageDTO sendMessage(Long groupId, String content, Long senderId) {
        // Inhalt validieren
        if (content == null || content.trim().isEmpty()) {
            throw ApiException.badRequest("Message content is required");
        }
        if (content.length() > MAX_MESSAGE_LENGTH) {
            throw ApiException.badRequest("Message content must be " + MAX_MESSAGE_LENGTH + " characters or less");
        }

        IdeaGroup group = em.find(IdeaGroup.class, groupId);
        if (group == null) {
            throw ApiException.notFound("Group not found");
        }

        // Verifizieren, dass Absender Mitglied ist
        if (!isMember(groupId, senderId)) {
            throw ApiException.forbidden("You are not a member of this group");
        }

        User sender = em.find(User.class, senderId);
        if (sender == null) {
            throw ApiException.notFound("User not found");
        }

        GroupMessage message = new GroupMessage();
        message.setGroup(group);
        message.setSender(sender);
        message.setContent(content.trim());

        em.persist(message);
        em.flush(); // Flushen, um sicherzustellen, dass ID generiert wird und @PrePersist ausgeführt wurde

        // updatedAt der Gruppe aktualisieren - aktuelle Zeit verwenden, wenn createdAt irgendwie null ist
        LocalDateTime updateTime = message.getCreatedAt() != null ? message.getCreatedAt() : LocalDateTime.now();
        group.setUpdatedAt(updateTime);
        em.merge(group);

        // Als gelesen vom Absender markieren (nur wenn Nachricht eine ID hat)
        if (message.getId() != null) {
            markMessageAsRead(message.getId(), senderId);
        }

        // Andere Gruppenmitglieder benachrichtigen
        notificationService.notifyGroupMessage(group, sender, content);

        return GroupMessageDTO.fromEntity(message);
    }

    /**
     * Markiert alle Nachrichten in einer Gruppe für einen Benutzer als gelesen.
     */
    @Transactional
    public void markAllMessagesAsRead(Long groupId, Long userId) {
        // Alle ungelesenen Nachrichten abrufen
        List<GroupMessage> messages = em.createNamedQuery("GroupMessage.findByGroup", GroupMessage.class)
                .setParameter("groupId", groupId)
                .getResultList();

        for (GroupMessage message : messages) {
            // Vom Benutzer gesendete Nachrichten überspringen
            if (message.getSender().getId().equals(userId)) {
                continue;
            }

            // Prüfen, ob bereits gelesen
            List<GroupMessageRead> reads = em.createNamedQuery("GroupMessageRead.findByMessageAndUser", GroupMessageRead.class)
                    .setParameter("messageId", message.getId())
                    .setParameter("userId", userId)
                    .getResultList();

            if (reads.isEmpty()) {
                markMessageAsRead(message.getId(), userId);
            }
        }
    }

    /**
     * Markiert eine einzelne Nachricht als gelesen.
     */
    @Transactional
    public void markMessageAsRead(Long messageId, Long userId) {
        GroupMessage message = em.find(GroupMessage.class, messageId);
        if (message == null) {
            return;
        }

        User user = em.find(User.class, userId);
        if (user == null) {
            return;
        }

        // Prüfen, ob bereits als gelesen markiert
        List<GroupMessageRead> existing = em.createNamedQuery("GroupMessageRead.findByMessageAndUser", GroupMessageRead.class)
                .setParameter("messageId", messageId)
                .setParameter("userId", userId)
                .getResultList();

        if (existing.isEmpty()) {
            GroupMessageRead read = new GroupMessageRead();
            read.setMessage(message);
            read.setUser(user);
            em.persist(read);
        }
    }

    /**
     * Prüft, ob ein Benutzer Mitglied einer Gruppe ist.
     */
    public boolean isMember(Long groupId, Long userId) {
        Long count = em.createQuery(
                "SELECT COUNT(gm) FROM GroupMember gm WHERE gm.group.id = :groupId AND gm.user.id = :userId", Long.class)
                .setParameter("groupId", groupId)
                .setParameter("userId", userId)
                .getSingleResult();
        return count > 0;
    }

    /**
     * Ruft die Anzahl ungelesener Nachrichten für einen Benutzer in einer Gruppe ab.
     */
    public int getUnreadMessageCount(Long groupId, Long userId) {
        Long count = em.createNamedQuery("GroupMessage.countUnreadByUser", Long.class)
                .setParameter("groupId", groupId)
                .setParameter("userId", userId)
                .getSingleResult();
        return count.intValue();
    }

    /**
     * Ruft die letzte Nachricht in einer Gruppe ab.
     */
    public GroupMessageDTO getLastMessage(Long groupId) {
        List<GroupMessage> messages = em.createNamedQuery("GroupMessage.findRecentByGroup", GroupMessage.class)
                .setParameter("groupId", groupId)
                .setMaxResults(1)
                .getResultList();

        if (messages.isEmpty()) {
            return null;
        }

        return GroupMessageDTO.fromEntity(messages.get(0));
    }

    /**
     * Ruft die Gesamtzahl ungelesener Gruppennachrichten für einen Benutzer über alle Gruppen hinweg ab.
     */
    public int getTotalUnreadCount(Long userId) {
        List<IdeaGroup> groups = em.createNamedQuery("IdeaGroup.findByUser", IdeaGroup.class)
                .setParameter("userId", userId)
                .getResultList();

        int total = 0;
        for (IdeaGroup group : groups) {
            total += getUnreadMessageCount(group.getId(), userId);
        }
        return total;
    }
}
