package com.gfos.ideaboard.dto;

import com.gfos.ideaboard.entity.Message;
import java.time.LocalDateTime;

public class MessageDTO {

    private Long id;
    private UserDTO sender;
    private UserDTO recipient;
    private Long ideaId;
    private String ideaTitle;
    private String content;
    private Boolean isRead;
    private LocalDateTime createdAt;

    public MessageDTO() {}

    public static MessageDTO fromEntity(Message message) {
        MessageDTO dto = new MessageDTO();
        dto.setId(message.getId());
        dto.setSender(UserDTO.fromEntity(message.getSender()));
        dto.setRecipient(UserDTO.fromEntity(message.getRecipient()));
        if (message.getIdea() != null) {
            dto.setIdeaId(message.getIdea().getId());
            dto.setIdeaTitle(message.getIdea().getTitle());
        }
        dto.setContent(message.getContent());
        dto.setIsRead(message.getIsRead());
        dto.setCreatedAt(message.getCreatedAt());
        return dto;
    }

    // Getters und Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public UserDTO getSender() {
        return sender;
    }

    public void setSender(UserDTO sender) {
        this.sender = sender;
    }

    public UserDTO getRecipient() {
        return recipient;
    }

    public void setRecipient(UserDTO recipient) {
        this.recipient = recipient;
    }

    public Long getIdeaId() {
        return ideaId;
    }

    public void setIdeaId(Long ideaId) {
        this.ideaId = ideaId;
    }

    public String getIdeaTitle() {
        return ideaTitle;
    }

    public void setIdeaTitle(String ideaTitle) {
        this.ideaTitle = ideaTitle;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Boolean getIsRead() {
        return isRead;
    }

    public void setIsRead(Boolean isRead) {
        this.isRead = isRead;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
