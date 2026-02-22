package com.gfos.ideaboard.dto;

import java.time.LocalDateTime;

public class ConversationDTO {

    private UserDTO otherUser;
    private MessageDTO lastMessage;
    private Long unreadCount;
    private LocalDateTime lastMessageAt;

    public ConversationDTO() {}

    public ConversationDTO(UserDTO otherUser, MessageDTO lastMessage, Long unreadCount) {
        this.otherUser = otherUser;
        this.lastMessage = lastMessage;
        this.unreadCount = unreadCount;
        this.lastMessageAt = lastMessage != null ? lastMessage.getCreatedAt() : null;
    }

    // Getters und Setters
    public UserDTO getOtherUser() {
        return otherUser;
    }

    public void setOtherUser(UserDTO otherUser) {
        this.otherUser = otherUser;
    }

    public MessageDTO getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(MessageDTO lastMessage) {
        this.lastMessage = lastMessage;
    }

    public Long getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(Long unreadCount) {
        this.unreadCount = unreadCount;
    }

    public LocalDateTime getLastMessageAt() {
        return lastMessageAt;
    }

    public void setLastMessageAt(LocalDateTime lastMessageAt) {
        this.lastMessageAt = lastMessageAt;
    }
}
