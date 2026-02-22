package com.gfos.ideaboard.dto;

import com.gfos.ideaboard.entity.Survey;
import com.gfos.ideaboard.entity.SurveyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class SurveyDTO {

    private Long id;
    private UserDTO creator;
    private String question;
    private String description;
    private List<SurveyOptionDTO> options;
    private Boolean isActive;
    private Boolean isAnonymous;
    private Boolean allowMultipleVotes;
    private Integer totalVotes;
    private Boolean hasVoted;
    private List<Long> userVotedOptionIds;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;

    public SurveyDTO() {}

    public static SurveyDTO fromEntity(Survey survey, List<Long> userVotedOptionIds) {
        SurveyDTO dto = new SurveyDTO();
        dto.setId(survey.getId());
        dto.setCreator(UserDTO.fromEntity(survey.getCreator()));
        dto.setQuestion(survey.getQuestion());
        dto.setDescription(survey.getDescription());
        dto.setOptions(survey.getOptions().stream()
                .map(SurveyOptionDTO::fromEntity)
                .collect(Collectors.toList()));
        dto.setIsActive(survey.getIsActive());
        dto.setIsAnonymous(survey.getIsAnonymous());
        dto.setAllowMultipleVotes(survey.getAllowMultipleVotes());
        dto.setTotalVotes(survey.getTotalVotes());
        dto.setHasVoted(userVotedOptionIds != null && !userVotedOptionIds.isEmpty());
        dto.setUserVotedOptionIds(userVotedOptionIds);
        dto.setExpiresAt(survey.getExpiresAt());
        dto.setCreatedAt(survey.getCreatedAt());

        // Prozentsätze berechnen
        if (dto.getTotalVotes() > 0) {
            dto.getOptions().forEach(opt ->
                    opt.setPercentage((double) opt.getVoteCount() / dto.getTotalVotes() * 100));
        }

        return dto;
    }

    // Getters und Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public UserDTO getCreator() {
        return creator;
    }

    public void setCreator(UserDTO creator) {
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

    public List<SurveyOptionDTO> getOptions() {
        return options;
    }

    public void setOptions(List<SurveyOptionDTO> options) {
        this.options = options;
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

    public Boolean getHasVoted() {
        return hasVoted;
    }

    public void setHasVoted(Boolean hasVoted) {
        this.hasVoted = hasVoted;
    }

    public List<Long> getUserVotedOptionIds() {
        return userVotedOptionIds;
    }

    public void setUserVotedOptionIds(List<Long> userVotedOptionIds) {
        this.userVotedOptionIds = userVotedOptionIds;
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

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public static class SurveyOptionDTO {
        private Long id;
        private String optionText;
        private Integer voteCount;
        private Double percentage;

        public SurveyOptionDTO() {}

        public static SurveyOptionDTO fromEntity(SurveyOption option) {
            SurveyOptionDTO dto = new SurveyOptionDTO();
            dto.setId(option.getId());
            dto.setOptionText(option.getOptionText());
            dto.setVoteCount(option.getVoteCount());
            return dto;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
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

        public Double getPercentage() {
            return percentage;
        }

        public void setPercentage(Double percentage) {
            this.percentage = percentage;
        }
    }
}
