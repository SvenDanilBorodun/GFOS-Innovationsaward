package com.gfos.ideaboard.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ideas")
@NamedQueries({
    @NamedQuery(name = "Idea.findAll", query = "SELECT i FROM Idea i ORDER BY i.createdAt DESC"),
    @NamedQuery(name = "Idea.findByCategory", query = "SELECT i FROM Idea i WHERE i.category = :category ORDER BY i.createdAt DESC"),
    @NamedQuery(name = "Idea.findByStatus", query = "SELECT i FROM Idea i WHERE i.status = :status ORDER BY i.createdAt DESC"),
    @NamedQuery(name = "Idea.findByAuthor", query = "SELECT i FROM Idea i WHERE i.author.id = :authorId ORDER BY i.createdAt DESC"),
    @NamedQuery(name = "Idea.findTopByLikes", query = "SELECT i FROM Idea i ORDER BY i.likeCount DESC"),
    @NamedQuery(name = "Idea.countByCategory", query = "SELECT i.category, COUNT(i) FROM Idea i GROUP BY i.category ORDER BY COUNT(i) DESC")
})
public class Idea {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 200)
    @Column(nullable = false, length = 200)
    private String title;

    @NotBlank
    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @NotBlank
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IdeaStatus status = IdeaStatus.CONCEPT;

    @Column(name = "progress_percentage", nullable = false)
    private Integer progressPercentage = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Column(name = "like_count", nullable = false)
    private Integer likeCount = 0;

    @Column(name = "comment_count", nullable = false)
    private Integer commentCount = 0;

    @Column(name = "view_count", nullable = false)
    private Integer viewCount = 0;

    @Column(name = "is_featured", nullable = false)
    private Boolean isFeatured = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Beziehungen
    @ElementCollection
    @CollectionTable(name = "idea_tags", joinColumns = @JoinColumn(name = "idea_id"))
    @Column(name = "tag_name")
    private List<String> tags = new ArrayList<>();

    @OneToMany(mappedBy = "idea", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FileAttachment> attachments = new ArrayList<>();

    @OneToMany(mappedBy = "idea", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Like> likes = new ArrayList<>();

    @OneToMany(mappedBy = "idea", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Comment> comments = new ArrayList<>();

    @OneToMany(mappedBy = "idea", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ordinalPosition ASC")
    private List<ChecklistItem> checklistItems = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
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
        this.progressPercentage = Math.max(0, Math.min(100, progressPercentage));
    }

    public User getAuthor() {
        return author;
    }

    public void setAuthor(User author) {
        this.author = author;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public List<FileAttachment> getAttachments() {
        return attachments;
    }

    public List<Like> getLikes() {
        return likes;
    }

    public List<Comment> getComments() {
        return comments;
    }

    public List<ChecklistItem> getChecklistItems() {
        return checklistItems;
    }

    public void incrementLikeCount() {
        this.likeCount++;
    }

    public void decrementLikeCount() {
        if (this.likeCount > 0) {
            this.likeCount--;
        }
    }

    public void incrementCommentCount() {
        this.commentCount++;
    }

    public void decrementCommentCount() {
        if (this.commentCount > 0) {
            this.commentCount--;
        }
    }

    public void incrementViewCount() {
        this.viewCount++;
    }
}
