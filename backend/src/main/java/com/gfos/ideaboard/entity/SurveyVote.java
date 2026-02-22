package com.gfos.ideaboard.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "survey_votes", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"survey_id", "user_id", "option_id"})
})
@NamedQueries({
    @NamedQuery(name = "SurveyVote.findByUserAndSurvey",
                query = "SELECT v FROM SurveyVote v WHERE v.user.id = :userId AND v.survey.id = :surveyId"),
    @NamedQuery(name = "SurveyVote.countByUserAndSurvey",
                query = "SELECT COUNT(v) FROM SurveyVote v WHERE v.user.id = :userId AND v.survey.id = :surveyId")
})
public class SurveyVote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "survey_id", nullable = false)
    private Survey survey;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "option_id", nullable = false)
    private SurveyOption option;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

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

    public Survey getSurvey() {
        return survey;
    }

    public void setSurvey(Survey survey) {
        this.survey = survey;
    }

    public SurveyOption getOption() {
        return option;
    }

    public void setOption(SurveyOption option) {
        this.option = option;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
