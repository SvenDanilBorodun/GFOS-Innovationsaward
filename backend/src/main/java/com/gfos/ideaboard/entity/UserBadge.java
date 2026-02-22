package com.gfos.ideaboard.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_badges", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "badge_id"})
})
@NamedQueries({
    @NamedQuery(name = "UserBadge.findByUser",
                query = "SELECT ub FROM UserBadge ub WHERE ub.user.id = :userId ORDER BY ub.earnedAt DESC"),
    @NamedQuery(name = "UserBadge.countByUserAndBadge",
                query = "SELECT COUNT(ub) FROM UserBadge ub WHERE ub.user.id = :userId AND ub.badge.id = :badgeId")
})
public class UserBadge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "badge_id", nullable = false)
    private Badge badge;

    @Column(name = "earned_at", nullable = false, updatable = false)
    private LocalDateTime earnedAt;

    @PrePersist
    protected void onCreate() {
        earnedAt = LocalDateTime.now();
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

    public Badge getBadge() {
        return badge;
    }

    public void setBadge(Badge badge) {
        this.badge = badge;
    }

    public LocalDateTime getEarnedAt() {
        return earnedAt;
    }
}
