package com.gfos.ideaboard.dto;

import com.gfos.ideaboard.entity.UserBadge;
import java.time.LocalDateTime;

public class UserBadgeDTO {

    private Long id;
    private BadgeDTO badge;
    private LocalDateTime earnedAt;

    public UserBadgeDTO() {}

    public static UserBadgeDTO fromEntity(UserBadge userBadge) {
        UserBadgeDTO dto = new UserBadgeDTO();
        dto.setId(userBadge.getId());
        dto.setBadge(BadgeDTO.fromEntity(userBadge.getBadge()));
        dto.setEarnedAt(userBadge.getEarnedAt());
        return dto;
    }

    // Getters und Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public BadgeDTO getBadge() {
        return badge;
    }

    public void setBadge(BadgeDTO badge) {
        this.badge = badge;
    }

    public LocalDateTime getEarnedAt() {
        return earnedAt;
    }

    public void setEarnedAt(LocalDateTime earnedAt) {
        this.earnedAt = earnedAt;
    }
}
