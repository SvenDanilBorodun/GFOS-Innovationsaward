package com.gfos.ideaboard.dto;

import com.gfos.ideaboard.entity.GroupMember;
import com.gfos.ideaboard.entity.IdeaGroup;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class IdeaGroupDTO {

    private Long id;
    private Long ideaId;
    private String ideaTitle;
    private String name;
    private String description;
    private UserDTO createdBy;
    private List<GroupMemberDTO> members;
    private Integer memberCount;
    private Integer unreadCount;
    private GroupMessageDTO lastMessage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public IdeaGroupDTO() {}

    public static IdeaGroupDTO fromEntity(IdeaGroup group) {
        return fromEntity(group, group.getMembers(), 0, null);
    }

    public static IdeaGroupDTO fromEntity(IdeaGroup group, int unreadCount, GroupMessageDTO lastMessage) {
        return fromEntity(group, group.getMembers(), unreadCount, lastMessage);
    }

    public static IdeaGroupDTO fromEntity(IdeaGroup group, List<GroupMember> members, int unreadCount, GroupMessageDTO lastMessage) {
        IdeaGroupDTO dto = new IdeaGroupDTO();
        dto.setId(group.getId());
        dto.setIdeaId(group.getIdea() != null ? group.getIdea().getId() : null);
        dto.setIdeaTitle(group.getIdea() != null ? group.getIdea().getTitle() : null);
        dto.setName(group.getName());
        dto.setDescription(group.getDescription());
        dto.setCreatedBy(group.getCreatedBy() != null ? UserDTO.fromEntity(group.getCreatedBy()) : null);
        dto.setMembers(members != null ? members.stream()
                .map(GroupMemberDTO::fromEntity)
                .collect(Collectors.toList()) : List.of());
        dto.setMemberCount(members != null ? members.size() : 0);
        dto.setUnreadCount(unreadCount);
        dto.setLastMessage(lastMessage);
        dto.setCreatedAt(group.getCreatedAt());
        dto.setUpdatedAt(group.getUpdatedAt());
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

    public String getIdeaTitle() {
        return ideaTitle;
    }

    public void setIdeaTitle(String ideaTitle) {
        this.ideaTitle = ideaTitle;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public UserDTO getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(UserDTO createdBy) {
        this.createdBy = createdBy;
    }

    public List<GroupMemberDTO> getMembers() {
        return members;
    }

    public void setMembers(List<GroupMemberDTO> members) {
        this.members = members;
    }

    public Integer getMemberCount() {
        return memberCount;
    }

    public void setMemberCount(Integer memberCount) {
        this.memberCount = memberCount;
    }

    public Integer getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(Integer unreadCount) {
        this.unreadCount = unreadCount;
    }

    public GroupMessageDTO getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(GroupMessageDTO lastMessage) {
        this.lastMessage = lastMessage;
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
