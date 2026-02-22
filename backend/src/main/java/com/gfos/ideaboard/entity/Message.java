package com.gfos.ideaboard.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

@Entity
@Table(name = "messages")
@NamedQueries({
    @NamedQuery(name = "Message.findConversation",
                query = "SELECT m FROM Message m WHERE (m.sender.id = :user1 AND m.recipient.id = :user2) OR (m.sender.id = :user2 AND m.recipient.id = :user1) ORDER BY m.createdAt ASC"),
    @NamedQuery(name = "Message.findUserMessages",
                query = "SELECT m FROM Message m WHERE m.sender.id = :userId OR m.recipient.id = :userId ORDER BY m.createdAt DESC"),
    @NamedQuery(name = "Message.findUnreadByRecipient",
                query = "SELECT m FROM Message m WHERE m.recipient.id = :userId AND m.isRead = false ORDER BY m.createdAt DESC"),
    @NamedQuery(name = "Message.countUnreadByRecipient",
                query = "SELECT COUNT(m) FROM Message m WHERE m.recipient.id = :userId AND m.isRead = false"),
    @NamedQuery(name = "Message.countUnreadFromUser",
                query = "SELECT COUNT(m) FROM Message m WHERE m.recipient.id = :recipientId AND m.sender.id = :senderId AND m.isRead = false"),
    @NamedQuery(name = "Message.findByIdea",
                query = "SELECT m FROM Message m WHERE m.idea.id = :ideaId ORDER BY m.createdAt DESC")
})
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_id", nullable = false)
    private User recipient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idea_id")
    private Idea idea;

    @NotBlank
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // Getters und Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getSender() {
        return sender;
    }

    public void setSender(User sender) {
        this.sender = sender;
    }

    public User getRecipient() {
        return recipient;
    }

    public void setRecipient(User recipient) {
        this.recipient = recipient;
    }

    public Idea getIdea() {
        return idea;
    }

    public void setIdea(Idea idea) {
        this.idea = idea;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Boolean getIsRead() {
        return isRead;
    }

    public void setIsRead(Boolean isRead) {
        this.isRead = isRead;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
