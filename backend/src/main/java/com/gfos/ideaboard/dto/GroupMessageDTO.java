package com.gfos.ideaboard.dto;

import com.gfos.ideaboard.entity.GroupMessage;
import java.time.LocalDateTime;

public class GroupMessageDTO {

    private Long id;
    private Long groupId;
    private UserDTO sender;
    private String content;
    private LocalDateTime createdAt;

    public GroupMessageDTO() {}

    public static GroupMessageDTO fromEntity(GroupMessage message) {
        GroupMessageDTO dto = new GroupMessageDTO();
        dto.setId(message.getId());
        dto.setGroupId(message.getGroup().getId());
        dto.setSender(UserDTO.fromEntity(message.getSender()));
        dto.setContent(message.getContent());
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

    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }

    public UserDTO getSender() {
        return sender;
    }

    public void setSender(UserDTO sender) {
        this.sender = sender;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
