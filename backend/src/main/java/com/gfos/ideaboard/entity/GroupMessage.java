package com.gfos.ideaboard.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

@Entity
@Table(name = "group_messages")
@NamedQueries({
    @NamedQuery(name = "GroupMessage.findByGroup",
                query = "SELECT gm FROM GroupMessage gm LEFT JOIN FETCH gm.sender WHERE gm.group.id = :groupId ORDER BY gm.createdAt ASC"),
    @NamedQuery(name = "GroupMessage.findRecentByGroup",
                query = "SELECT gm FROM GroupMessage gm LEFT JOIN FETCH gm.sender WHERE gm.group.id = :groupId ORDER BY gm.createdAt DESC"),
    @NamedQuery(name = "GroupMessage.countUnreadByUser",
                query = "SELECT COUNT(gm) FROM GroupMessage gm WHERE gm.group.id = :groupId AND gm.sender.id != :userId AND gm.id NOT IN (SELECT gmr.message.id FROM GroupMessageRead gmr WHERE gmr.user.id = :userId)")
})
public class GroupMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private IdeaGroup group;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @NotBlank
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

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

    public IdeaGroup getGroup() {
        return group;
    }

    public void setGroup(IdeaGroup group) {
        this.group = group;
    }

    public User getSender() {
        return sender;
    }

    public void setSender(User sender) {
        this.sender = sender;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
