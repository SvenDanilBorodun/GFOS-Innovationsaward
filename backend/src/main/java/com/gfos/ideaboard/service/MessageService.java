package com.gfos.ideaboard.service;

import com.gfos.ideaboard.dto.ConversationDTO;
import com.gfos.ideaboard.dto.MessageDTO;
import com.gfos.ideaboard.dto.UserDTO;
import com.gfos.ideaboard.entity.*;
import com.gfos.ideaboard.exception.ApiException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.Response;
import java.util.*;
import java.util.stream.Collectors;

@ApplicationScoped
public class MessageService {

    @PersistenceContext(unitName = "IdeaBoardPU")
    private EntityManager em;

    @Inject
    private NotificationService notificationService;

    @Transactional
    public MessageDTO sendMessage(Long senderId, Long recipientId, String content, Long ideaId) {
        if (senderId.equals(recipientId)) {
            throw ApiException.badRequest("Cannot send message to yourself");
        }

        User sender = em.find(User.class, senderId);
        if (sender == null) {
            throw ApiException.notFound("Sender not found");
        }

        User recipient = em.find(User.class, recipientId);
        if (recipient == null) {
            throw ApiException.notFound("Recipient not found");
        }

        Message message = new Message();
        message.setSender(sender);
        message.setRecipient(recipient);
        message.setContent(content);

        if (ideaId != null) {
            Idea idea = em.find(Idea.class, ideaId);
            if (idea != null) {
                message.setIdea(idea);
            }
        }

        em.persist(message);

        // Benachrichtigung für Empfänger erstellen
        notifyNewMessage(recipient, sender, content, ideaId);

        return MessageDTO.fromEntity(message);
    }

    public List<MessageDTO> getConversation(Long userId, Long otherUserId, int limit, int offset) {
        List<Message> messages = em.createNamedQuery("Message.findConversation", Message.class)
                .setParameter("user1", userId)
                .setParameter("user2", otherUserId)
                .setFirstResult(offset)
                .setMaxResults(limit)
                .getResultList();

        return messages.stream()
                .map(MessageDTO::fromEntity)
                .collect(Collectors.toList());
    }

    public List<ConversationDTO> getUserConversations(Long userId) {
        // Alle Nachrichten für diesen Benutzer abrufen
        List<Message> allMessages = em.createNamedQuery("Message.findUserMessages", Message.class)
                .setParameter("userId", userId)
                .getResultList();

        // Nachrichten nach Gesprächspartner gruppieren
        Map<Long, List<Message>> messagesByUser = new LinkedHashMap<>();

        for (Message msg : allMessages) {
            Long otherUserId = msg.getSender().getId().equals(userId)
                    ? msg.getRecipient().getId()
                    : msg.getSender().getId();

            messagesByUser.computeIfAbsent(otherUserId, k -> new ArrayList<>()).add(msg);
        }

        // Konversations-DTOs erstellen
        List<ConversationDTO> conversations = new ArrayList<>();

        for (Map.Entry<Long, List<Message>> entry : messagesByUser.entrySet()) {
            Long otherUserId = entry.getKey();
            List<Message> convMessages = entry.getValue();

            // Den anderen Benutzer abrufen
            User otherUser = em.find(User.class, otherUserId);
            if (otherUser == null) continue;

            // Die aktuellste Nachricht abrufen
            Message lastMessage = convMessages.get(0);

            // Ungelesene Nachrichten von diesem Benutzer zählen
            Long unreadCount = em.createNamedQuery("Message.countUnreadFromUser", Long.class)
                    .setParameter("recipientId", userId)
                    .setParameter("senderId", otherUserId)
                    .getSingleResult();

            ConversationDTO conv = new ConversationDTO(
                    UserDTO.fromEntity(otherUser),
                    MessageDTO.fromEntity(lastMessage),
                    unreadCount
            );
            conversations.add(conv);
        }

        // Nach Datum der letzten Nachricht sortieren (aktuellste zuerst)
        conversations.sort((a, b) -> {
            if (a.getLastMessageAt() == null) return 1;
            if (b.getLastMessageAt() == null) return -1;
            return b.getLastMessageAt().compareTo(a.getLastMessageAt());
        });

        return conversations;
    }

    public long getUnreadCount(Long userId) {
        return em.createNamedQuery("Message.countUnreadByRecipient", Long.class)
                .setParameter("userId", userId)
                .getSingleResult();
    }

    @Transactional
    public void markAsRead(Long messageId, Long userId) {
        Message message = em.find(Message.class, messageId);
        if (message != null && message.getRecipient().getId().equals(userId)) {
            message.setIsRead(true);
            em.merge(message);
        }
    }

    @Transactional
    public void markConversationAsRead(Long userId, Long otherUserId) {
        em.createQuery("UPDATE Message m SET m.isRead = true WHERE m.recipient.id = :userId AND m.sender.id = :otherUserId AND m.isRead = false")
                .setParameter("userId", userId)
                .setParameter("otherUserId", otherUserId)
                .executeUpdate();
    }

    public List<MessageDTO> getMessagesByIdea(Long ideaId, int limit) {
        List<Message> messages = em.createNamedQuery("Message.findByIdea", Message.class)
                .setParameter("ideaId", ideaId)
                .setMaxResults(limit)
                .getResultList();

        return messages.stream()
                .map(MessageDTO::fromEntity)
                .collect(Collectors.toList());
    }

    private void notifyNewMessage(User recipient, User sender, String content, Long ideaId) {
        Notification notification = new Notification();
        notification.setUser(recipient);
        notification.setType(NotificationType.MESSAGE);
        notification.setTitle("New Message");
        notification.setMessage(sender.getFirstName() + " " + sender.getLastName() + " sent you a message: " + truncate(content, 50));
        notification.setLink("/messages?user=" + sender.getId());
        notification.setSender(sender);
        notification.setRelatedEntityType("Message");
        if (ideaId != null) {
            notification.setRelatedEntityId(ideaId);
        }
        em.persist(notification);
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength - 3) + "...";
    }
}
