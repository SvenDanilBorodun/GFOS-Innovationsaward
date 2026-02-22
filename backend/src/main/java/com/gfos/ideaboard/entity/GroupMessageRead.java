package com.gfos.ideaboard.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "group_message_reads", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"message_id", "user_id"})
})
@NamedQueries({
    @NamedQuery(name = "GroupMessageRead.findByMessageAndUser",
                query = "SELECT gmr FROM GroupMessageRead gmr WHERE gmr.message.id = :messageId AND gmr.user.id = :userId")
})
public class GroupMessageRead {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", nullable = false)
    private GroupMessage message;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "read_at", nullable = false, updatable = false)
    private LocalDateTime readAt;

    @PrePersist
    protected void onCreate() {
        readAt = LocalDateTime.now();
    }

    // Getters und Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public GroupMessage getMessage() {
        return message;
    }

    public void setMessage(GroupMessage message) {
        this.message = message;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public LocalDateTime getReadAt() {
        return readAt;
    }
}
