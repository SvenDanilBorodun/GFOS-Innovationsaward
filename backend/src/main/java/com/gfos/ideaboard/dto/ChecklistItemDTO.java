package com.gfos.ideaboard.dto;

import com.gfos.ideaboard.entity.ChecklistItem;
import java.time.LocalDateTime;

public class ChecklistItemDTO {

    private Long id;
    private Long ideaId;
    private String title;
    private Boolean isCompleted;
    private Integer ordinalPosition;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public ChecklistItemDTO() {}

    public static ChecklistItemDTO fromEntity(ChecklistItem item) {
        ChecklistItemDTO dto = new ChecklistItemDTO();
        dto.setId(item.getId());
        dto.setIdeaId(item.getIdea().getId());
        dto.setTitle(item.getTitle());
        dto.setIsCompleted(item.getIsCompleted());
        dto.setOrdinalPosition(item.getOrdinalPosition());
        dto.setCreatedAt(item.getCreatedAt());
        dto.setUpdatedAt(item.getUpdatedAt());
        return dto;
    }

    // Getters und Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getIdeaId() {
        return ideaId;
    }

    public void setIdeaId(Long ideaId) {
        this.ideaId = ideaId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Boolean getIsCompleted() {
        return isCompleted;
    }

    public void setIsCompleted(Boolean isCompleted) {
        this.isCompleted = isCompleted;
    }

    public Integer getOrdinalPosition() {
        return ordinalPosition;
    }

    public void setOrdinalPosition(Integer ordinalPosition) {
        this.ordinalPosition = ordinalPosition;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
