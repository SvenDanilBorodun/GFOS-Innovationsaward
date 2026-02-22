package com.gfos.ideaboard.dto;

import com.gfos.ideaboard.entity.GroupMember;
import com.gfos.ideaboard.entity.GroupMemberRole;
import java.time.LocalDateTime;

public class GroupMemberDTO {

    private Long id;
    private Long groupId;
    private UserDTO user;
    private GroupMemberRole role;
    private LocalDateTime joinedAt;

    public GroupMemberDTO() {}

    public static GroupMemberDTO fromEntity(GroupMember member) {
        GroupMemberDTO dto = new GroupMemberDTO();
        dto.setId(member.getId());
        dto.setGroupId(member.getGroup().getId());
        dto.setUser(UserDTO.fromEntity(member.getUser()));
        dto.setRole(member.getRole());
        dto.setJoinedAt(member.getJoinedAt());
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

    public UserDTO getUser() {
        return user;
    }

    public void setUser(UserDTO user) {
        this.user = user;
    }

    public GroupMemberRole getRole() {
        return role;
    }

    public void setRole(GroupMemberRole role) {
        this.role = role;
    }

    public LocalDateTime getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(LocalDateTime joinedAt) {
        this.joinedAt = joinedAt;
    }
}
