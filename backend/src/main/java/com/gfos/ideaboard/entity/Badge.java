package com.gfos.ideaboard.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

@Entity
@Table(name = "badges")
@NamedQueries({
    @NamedQuery(name = "Badge.findActive",
                query = "SELECT b FROM Badge b WHERE b.isActive = true ORDER BY b.name"),
    @NamedQuery(name = "Badge.findByName",
                query = "SELECT b FROM Badge b WHERE b.name = :name")
})
public class Badge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 100)
    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Size(max = 100)
    @Column(name = "display_name", length = 100)
    private String displayName;

    @NotBlank
    @Size(max = 500)
    @Column(nullable = false, length = 500)
    private String description;

    @NotBlank
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String icon;

    @Size(max = 500)
    @Column(length = 500)
    private String criteria;

    @Column(name = "xp_reward", nullable = false)
    private Integer xpReward = 0;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDisplayName() {
        return displayName != null ? displayName : name;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getCriteria() {
        return criteria;
    }

    public void setCriteria(String criteria) {
        this.criteria = criteria;
    }

    public Integer getXpReward() {
        return xpReward;
    }

    public void setXpReward(Integer xpReward) {
        this.xpReward = xpReward;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
