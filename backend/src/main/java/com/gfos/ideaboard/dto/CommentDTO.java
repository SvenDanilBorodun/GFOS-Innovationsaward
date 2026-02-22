package com.gfos.ideaboard.dto;

import com.gfos.ideaboard.entity.Comment;
import com.gfos.ideaboard.entity.CommentReaction;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CommentDTO {

    private Long id;
    private Long ideaId;
    private UserDTO author;
    private String content;
    private Integer reactionCount;
    private List<ReactionDTO> reactions;
    private List<String> currentUserReactionEmojis;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public CommentDTO() {}

    public static CommentDTO fromEntity(Comment comment, Long currentUserId) {
        CommentDTO dto = new CommentDTO();
        dto.setId(comment.getId());
        dto.setIdeaId(comment.getIdea().getId());
        dto.setAuthor(UserDTO.fromEntity(comment.getAuthor()));
        dto.setContent(comment.getContent());
        dto.setReactionCount(comment.getReactionCount());
        dto.setCreatedAt(comment.getCreatedAt());
        dto.setUpdatedAt(comment.getUpdatedAt());

        // Reaktionen nach Emoji gruppieren
        Map<String, Long> reactionCounts = comment.getReactions().stream()
                .collect(Collectors.groupingBy(CommentReaction::getEmoji, Collectors.counting()));

        dto.setReactions(reactionCounts.entrySet().stream()
                .map(entry -> new ReactionDTO(entry.getKey(), entry.getValue().intValue()))
                .collect(Collectors.toList()));

        // Reaktions-Emojis des aktuellen Benutzers sammeln
        if (currentUserId != null) {
            List<String> userEmojis = comment.getReactions().stream()
                    .filter(reaction -> reaction.getUser().getId().equals(currentUserId))
                    .map(CommentReaction::getEmoji)
                    .collect(Collectors.toList());
            dto.setCurrentUserReactionEmojis(userEmojis);
        } else {
            dto.setCurrentUserReactionEmojis(new ArrayList<>());
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

    public Long getIdeaId() {
        return ideaId;
    }

    public void setIdeaId(Long ideaId) {
        this.ideaId = ideaId;
    }

    public UserDTO getAuthor() {
        return author;
    }

    public void setAuthor(UserDTO author) {
        this.author = author;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Integer getReactionCount() {
        return reactionCount;
    }

    public void setReactionCount(Integer reactionCount) {
        this.reactionCount = reactionCount;
    }

    public List<ReactionDTO> getReactions() {
        return reactions;
    }

    public void setReactions(List<ReactionDTO> reactions) {
        this.reactions = reactions;
    }

    public List<String> getCurrentUserReactionEmojis() {
        return currentUserReactionEmojis;
    }

    public void setCurrentUserReactionEmojis(List<String> currentUserReactionEmojis) {
        this.currentUserReactionEmojis = currentUserReactionEmojis;
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

    public static class ReactionDTO {
        private String emoji;
        private Integer count;

        public ReactionDTO() {}

        public ReactionDTO(String emoji, Integer count) {
            this.emoji = emoji;
            this.count = count;
        }

        public String getEmoji() {
            return emoji;
        }

        public void setEmoji(String emoji) {
            this.emoji = emoji;
        }

        public Integer getCount() {
            return count;
        }

        public void setCount(Integer count) {
            this.count = count;
        }
    }
}
