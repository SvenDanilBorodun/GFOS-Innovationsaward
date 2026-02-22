package com.gfos.ideaboard.dto;

import com.gfos.ideaboard.entity.Badge;

public class BadgeDTO {

    private Long id;
    private String name;
    private String displayName;
    private String description;
    private String icon;
    private String criteria;
    private Integer xpReward;

    public BadgeDTO() {}

    public static BadgeDTO fromEntity(Badge badge) {
        BadgeDTO dto = new BadgeDTO();
        dto.setId(badge.getId());
        dto.setName(badge.getName());
        dto.setDisplayName(badge.getDisplayName());
        dto.setDescription(badge.getDescription());
        dto.setIcon(badge.getIcon());
        dto.setCriteria(badge.getCriteria());
        dto.setXpReward(badge.getXpReward());
        return dto;
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
        return displayName;
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
}
