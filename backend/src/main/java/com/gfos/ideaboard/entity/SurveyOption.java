package com.gfos.ideaboard.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "survey_options")
public class SurveyOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "survey_id", nullable = false)
    private Survey survey;

    @NotBlank
    @Size(max = 200)
    @Column(name = "option_text", nullable = false, length = 200)
    private String optionText;

    @Column(name = "vote_count", nullable = false)
    private Integer voteCount = 0;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;

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

    public String getOptionText() {
        return optionText;
    }

    public void setOptionText(String optionText) {
        this.optionText = optionText;
    }

    public Integer getVoteCount() {
        return voteCount;
    }

    public void setVoteCount(Integer voteCount) {
        this.voteCount = voteCount;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }

    public void incrementVoteCount() {
        this.voteCount++;
    }

    public void decrementVoteCount() {
        if (this.voteCount > 0) {
            this.voteCount--;
        }
    }
}
