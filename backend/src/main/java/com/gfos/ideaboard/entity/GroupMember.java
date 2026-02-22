package com.gfos.ideaboard.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "group_members", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"group_id", "user_id"})
})
@NamedQueries({
    @NamedQuery(name = "GroupMember.findByGroupAndUser",
                query = "SELECT gm FROM GroupMember gm WHERE gm.group.id = :groupId AND gm.user.id = :userId"),
    @NamedQuery(name = "GroupMember.findByGroup",
                query = "SELECT gm FROM GroupMember gm LEFT JOIN FETCH gm.user WHERE gm.group.id = :groupId ORDER BY gm.joinedAt ASC"),
    @NamedQuery(name = "GroupMember.findByGroupWithUser",
                query = "SELECT gm FROM GroupMember gm LEFT JOIN FETCH gm.user WHERE gm.group.id = :groupId ORDER BY gm.joinedAt ASC"),
    @NamedQuery(name = "GroupMember.countByGroup",
                query = "SELECT COUNT(gm) FROM GroupMember gm WHERE gm.group.id = :groupId")
})
public class GroupMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private IdeaGroup group;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GroupMemberRole role = GroupMemberRole.MEMBER;

    @Column(name = "joined_at", nullable = false, updatable = false)
    private LocalDateTime joinedAt;

    @PrePersist
    protected void onCreate() {
        joinedAt = LocalDateTime.now();
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

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public GroupMemberRole getRole() {
        return role;
    }

    public void setRole(GroupMemberRole role) {
        this.role = role;
    }

    public LocalDateTime getJoinedAt() {
        return joinedAt;
    }
}
