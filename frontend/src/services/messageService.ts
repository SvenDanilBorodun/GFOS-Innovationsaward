import api from './api';
import { Message, Conversation, SendMessageRequest } from '../types';

export const messageService = {
  async sendMessage(data: SendMessageRequest): Promise<Message> {
    const response = await api.post<Message>('/messages', data);
    return response.data;
  },

  async getConversations(): Promise<Conversation[]> {
    const response = await api.get<Conversation[]>('/messages/conversations');
    return response.data;
  },

  async getConversation(userId: number, limit: number = 50, offset: number = 0): Promise<Message[]> {
    const params = new URLSearchParams();
    params.append('limit', limit.toString());
    params.append('offset', offset.toString());
    const response = await api.get<Message[]>(`/messages/conversations/${userId}?${params.toString()}`);
    return response.data;
  },

  async getUnreadCount(): Promise<{ count: number }> {
    const response = await api.get<{ count: number }>('/messages/unread-count');
    return response.data;
  },

  async markAsRead(messageId: number): Promise<void> {
    await api.put(`/messages/${messageId}/read`);
  },

  async markConversationAsRead(userId: number): Promise<void> {
    await api.put(`/messages/conversations/${userId}/read`);
  },

  async getMessagesByIdea(ideaId: number, limit: number = 50): Promise<Message[]> {
    const response = await api.get<Message[]>(`/messages/idea/${ideaId}?limit=${limit}`);
    return response.data;
  },
};
