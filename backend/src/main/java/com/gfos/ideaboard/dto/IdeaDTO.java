package com.gfos.ideaboard.dto;

import com.gfos.ideaboard.entity.Idea;
import com.gfos.ideaboard.entity.IdeaStatus;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class IdeaDTO {

    private Long id;
    private String title;
    private String description;
    private String category;
    private IdeaStatus status;
    private Integer progressPercentage;
    private UserDTO author;
    private List<String> tags;
    private List<FileAttachmentDTO> attachments;
    private Integer likeCount;
    private Integer commentCount;
    private Integer viewCount;
    private Boolean isFeatured;
    private Boolean isLikedByCurrentUser;
    private List<ChecklistItemDTO> checklistItems = new ArrayList<>();
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public IdeaDTO() {}

    public static IdeaDTO fromEntity(Idea idea) {
        return fromEntity(idea, false);
    }

    public static IdeaDTO fromEntity(Idea idea, boolean isLikedByCurrentUser) {
        IdeaDTO dto = new IdeaDTO();
        dto.setId(idea.getId());
        dto.setTitle(idea.getTitle());
        dto.setDescription(idea.getDescription());
        dto.setCategory(idea.getCategory());
        dto.setStatus(idea.getStatus());
        dto.setProgressPercentage(idea.getProgressPercentage());
        dto.setAuthor(UserDTO.fromEntity(idea.getAuthor()));
        dto.setTags(idea.getTags());
        dto.setAttachments(idea.getAttachments().stream()
                .map(FileAttachmentDTO::fromEntity)
                .collect(Collectors.toList()));
        dto.setLikeCount(idea.getLikeCount());
        dto.setCommentCount(idea.getCommentCount());
        dto.setViewCount(idea.getViewCount());
        dto.setIsFeatured(idea.getIsFeatured());
        dto.setIsLikedByCurrentUser(isLikedByCurrentUser);
        dto.setChecklistItems(idea.getChecklistItems().stream()
                .map(ChecklistItemDTO::fromEntity)
                .collect(Collectors.toList()));
        dto.setCreatedAt(idea.getCreatedAt());
        dto.setUpdatedAt(idea.getUpdatedAt());
        return dto;
    }

    // Getters und Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public IdeaStatus getStatus() {
        return status;
    }

    public void setStatus(IdeaStatus status) {
        this.status = status;
    }

    public Integer getProgressPercentage() {
        return progressPercentage;
    }

    public void setProgressPercentage(Integer progressPercentage) {
        this.progressPercentage = progressPercentage;
    }

    public UserDTO getAuthor() {
        return author;
    }

    public void setAuthor(UserDTO author) {
        this.author = author;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public List<FileAttachmentDTO> getAttachments() {
        return attachments;
    }

    public void setAttachments(List<FileAttachmentDTO> attachments) {
        this.attachments = attachments;
    }

    public Integer getLikeCount() {
        return likeCount;
    }

    public void setLikeCount(Integer likeCount) {
        this.likeCount = likeCount;
    }

    public Integer getCommentCount() {
        return commentCount;
    }

    public void setCommentCount(Integer commentCount) {
        this.commentCount = commentCount;
    }

    public Integer getViewCount() {
        return viewCount;
    }

    public void setViewCount(Integer viewCount) {
        this.viewCount = viewCount;
    }

    public Boolean getIsFeatured() {
        return isFeatured;
    }

    public void setIsFeatured(Boolean isFeatured) {
        this.isFeatured = isFeatured;
    }

    public Boolean getIsLikedByCurrentUser() {
        return isLikedByCurrentUser;
    }

    public void setIsLikedByCurrentUser(Boolean isLikedByCurrentUser) {
        this.isLikedByCurrentUser = isLikedByCurrentUser;
    }

    public List<ChecklistItemDTO> getChecklistItems() {
        return checklistItems;
    }

    public void setChecklistItems(List<ChecklistItemDTO> checklistItems) {
        this.checklistItems = checklistItems;
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
