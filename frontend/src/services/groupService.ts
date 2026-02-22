import api from './api';
import { IdeaGroup, GroupMessage } from '../types';

export const groupService = {
  // Alle Gruppen abrufen, in denen der Benutzer Mitglied ist
  async getUserGroups(): Promise<IdeaGroup[]> {
    const response = await api.get<IdeaGroup[]>('/groups');
    return response.data;
  },

  // Eine bestimmte Gruppe per ID abrufen
  async getGroup(groupId: number): Promise<IdeaGroup> {
    const response = await api.get<IdeaGroup>(`/groups/${groupId}`);
    return response.data;
  },

  // Gruppe anhand der Ideen-ID abrufen
  async getGroupByIdea(ideaId: number): Promise<IdeaGroup> {
    const response = await api.get<IdeaGroup>(`/groups/idea/${ideaId}`);
    return response.data;
  },

  // Einer Gruppe beitreten
  async joinGroup(groupId: number): Promise<IdeaGroup> {
    const response = await api.post<IdeaGroup>(`/groups/${groupId}/join`);
    return response.data;
  },

  // Einer Gruppe anhand der Ideen-ID beitreten
  async joinGroupByIdea(ideaId: number): Promise<IdeaGroup> {
    const response = await api.post<IdeaGroup>(`/groups/idea/${ideaId}/join`);
    return response.data;
  },

  // Eine Gruppe verlassen
  async leaveGroup(groupId: number): Promise<void> {
    await api.delete(`/groups/${groupId}/leave`);
  },

  // Alle Nachrichten einer Gruppe abrufen
  async getGroupMessages(groupId: number): Promise<GroupMessage[]> {
    const response = await api.get<GroupMessage[]>(`/groups/${groupId}/messages`);
    return response.data;
  },

  // Eine Nachricht an eine Gruppe senden
  async sendGroupMessage(groupId: number, content: string): Promise<GroupMessage> {
    const response = await api.post<GroupMessage>(`/groups/${groupId}/messages`, { content });
    return response.data;
  },

  // Alle Nachrichten einer Gruppe als gelesen markieren
  async markAllAsRead(groupId: number): Promise<void> {
    await api.put(`/groups/${groupId}/messages/read`);
  },

  // Prüfen, ob der Benutzer Mitglied einer Gruppe ist
  async checkMembership(groupId: number): Promise<{ isMember: boolean }> {
    const response = await api.get<{ isMember: boolean }>(`/groups/${groupId}/membership`);
    return response.data;
  },

  // Prüfen, ob der Benutzer anhand der Ideen-ID Mitglied einer Gruppe ist
  async checkMembershipByIdea(ideaId: number): Promise<{ isMember: boolean; groupId?: number }> {
    const response = await api.get<{ isMember: boolean; groupId?: number }>(`/groups/idea/${ideaId}/membership`);
    return response.data;
  },

  // Gesamtzahl ungelesener Nachrichten über alle Gruppen hinweg abrufen
  async getTotalUnreadCount(): Promise<{ unreadCount: number }> {
    const response = await api.get<{ unreadCount: number }>('/groups/unread-count');
    return response.data;
  },
};
