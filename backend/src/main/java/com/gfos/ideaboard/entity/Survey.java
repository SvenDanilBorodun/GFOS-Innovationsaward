package com.gfos.ideaboard.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "surveys")
@NamedQueries({
    @NamedQuery(name = "Survey.findActive",
                query = "SELECT s FROM Survey s WHERE s.isActive = true ORDER BY s.createdAt DESC"),
    @NamedQuery(name = "Survey.findByCreator",
                query = "SELECT s FROM Survey s WHERE s.creator.id = :creatorId ORDER BY s.createdAt DESC")
})
public class Survey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false)
    private User creator;

    @NotBlank
    @Size(max = 500)
    @Column(nullable = false, length = 500)
    private String question;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "is_anonymous", nullable = false)
    private Boolean isAnonymous = false;

    @Column(name = "allow_multiple_votes", nullable = false)
    private Boolean allowMultipleVotes = false;

    @Column(name = "total_votes", nullable = false)
    private Integer totalVotes = 0;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "survey", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC")
    private List<SurveyOption> options = new ArrayList<>();

    @OneToMany(mappedBy = "survey", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SurveyVote> votes = new ArrayList<>();

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

    public User getCreator() {
        return creator;
    }

    public void setCreator(User creator) {
        this.creator = creator;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public Boolean getIsAnonymous() {
        return isAnonymous;
    }

    public void setIsAnonymous(Boolean isAnonymous) {
        this.isAnonymous = isAnonymous;
    }

    public Boolean getAllowMultipleVotes() {
        return allowMultipleVotes;
    }

    public void setAllowMultipleVotes(Boolean allowMultipleVotes) {
        this.allowMultipleVotes = allowMultipleVotes;
    }

    public Integer getTotalVotes() {
        return totalVotes;
    }

    public void setTotalVotes(Integer totalVotes) {
        this.totalVotes = totalVotes;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public List<SurveyOption> getOptions() {
        return options;
    }

    public void setOptions(List<SurveyOption> options) {
        this.options = options;
    }

    public List<SurveyVote> getVotes() {
        return votes;
    }

    public void addOption(SurveyOption option) {
        options.add(option);
        option.setSurvey(this);
    }

    public void incrementTotalVotes() {
        this.totalVotes++;
    }

    public void decrementTotalVotes() {
        if (this.totalVotes > 0) {
            this.totalVotes--;
        }
    }
}
