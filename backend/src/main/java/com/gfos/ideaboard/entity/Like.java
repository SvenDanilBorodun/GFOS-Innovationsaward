package com.gfos.ideaboard.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "likes", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "idea_id"})
})
@NamedQueries({
    @NamedQuery(name = "Like.findByUserAndIdea",
                query = "SELECT l FROM Like l WHERE l.user.id = :userId AND l.idea.id = :ideaId"),
    @NamedQuery(name = "Like.countByUserSince",
                query = "SELECT COUNT(l) FROM Like l WHERE l.user.id = :userId AND l.createdAt >= :since"),
    @NamedQuery(name = "Like.countByIdeaSince",
                query = "SELECT COUNT(l) FROM Like l WHERE l.idea.id = :ideaId AND l.createdAt >= :since")
})
public class Like {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idea_id", nullable = false)
    private Idea idea;

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

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Idea getIdea() {
        return idea;
    }

    public void setIdea(Idea idea) {
        this.idea = idea;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
