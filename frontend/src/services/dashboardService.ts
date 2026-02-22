import api from './api';
import { DashboardStats, TopIdea, Idea, Survey, Notification } from '../types';

export const dashboardService = {
  async getStats(): Promise<DashboardStats> {
    const response = await api.get<DashboardStats>('/dashboard/statistics');
    return response.data;
  },

  async getTopIdeas(): Promise<TopIdea[]> {
    const response = await api.get<TopIdea[]>('/dashboard/top-ideas');
    return response.data;
  },

  async getNewIdeas(limit: number = 5): Promise<Idea[]> {
    const response = await api.get<Idea[]>(`/dashboard/new-ideas?limit=${limit}`);
    return response.data;
  },

  async getActiveSurveys(): Promise<Survey[]> {
    const response = await api.get<Survey[]>('/dashboard/surveys');
    return response.data;
  },

  // Benachrichtigungen
  async getNotifications(): Promise<Notification[]> {
    const response = await api.get<Notification[]>('/notifications');
    return response.data;
  },

  async getUnreadCount(): Promise<number> {
    const response = await api.get<{ count: number }>('/notifications/unread-count');
    return response.data.count;
  },

  async markAsRead(notificationId: number): Promise<void> {
    await api.put(`/notifications/${notificationId}/read`);
  },

  async markAllAsRead(): Promise<void> {
    await api.put('/notifications/read-all');
  },
};
