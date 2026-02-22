package com.gfos.ideaboard.dto;

import com.gfos.ideaboard.entity.Notification;
import com.gfos.ideaboard.entity.NotificationType;
import java.time.LocalDateTime;

public class NotificationDTO {

    private Long id;
    private NotificationType type;
    private String title;
    private String message;
    private String link;
    private Boolean isRead;
    private UserDTO sender;
    private LocalDateTime createdAt;

    public NotificationDTO() {}

    public static NotificationDTO fromEntity(Notification notification) {
        NotificationDTO dto = new NotificationDTO();
        dto.setId(notification.getId());
        dto.setType(notification.getType());
        dto.setTitle(notification.getTitle());
        dto.setMessage(notification.getMessage());
        dto.setLink(notification.getLink());
        dto.setIsRead(notification.getIsRead());
        if (notification.getSender() != null) {
            dto.setSender(UserDTO.fromEntity(notification.getSender()));
        }
        dto.setCreatedAt(notification.getCreatedAt());
        return dto;
    }

    // Getters und Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public NotificationType getType() {
        return type;
    }

    public void setType(NotificationType type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public Boolean getIsRead() {
        return isRead;
    }

    public void setIsRead(Boolean isRead) {
        this.isRead = isRead;
    }

    public UserDTO getSender() {
        return sender;
    }

    public void setSender(UserDTO sender) {
        this.sender = sender;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
