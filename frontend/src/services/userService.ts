import api from './api';
import { User, UserBadge, Badge } from '../types';

const userService = {
  async getCurrentUser(): Promise<User> {
    const response = await api.get<User>('/users/me');
    return response.data;
  },

  async updateCurrentUser(data: Partial<User>): Promise<User> {
    const response = await api.put<User>('/users/me', data);
    return response.data;
  },

  async getUserById(id: number): Promise<User> {
    const response = await api.get<User>(`/users/${id}`);
    return response.data;
  },

  async getAllUsers(): Promise<User[]> {
    const response = await api.get<User[]>('/users');
    return response.data;
  },

  async updateUserRole(userId: number, role: string): Promise<void> {
    await api.put(`/users/${userId}/role`, { role });
  },

  async setUserActive(userId: number, isActive: boolean): Promise<void> {
    await api.put(`/users/${userId}/status`, { isActive });
  },

  async getRemainingLikes(): Promise<{ remaining: number; used: number }> {
    const response = await api.get<{ remaining: number; used: number }>('/users/me/likes/remaining');
    return response.data;
  },

  async getUserBadges(userId: number): Promise<UserBadge[]> {
    const response = await api.get<UserBadge[]>(`/users/${userId}/badges`);
    return response.data;
  },

  async getCurrentUserBadges(): Promise<UserBadge[]> {
    const response = await api.get<UserBadge[]>('/users/me/badges');
    return response.data;
  },

  async getAllBadges(): Promise<Badge[]> {
    const response = await api.get<Badge[]>('/users/badges');
    return response.data;
  },

  async getLeaderboard(limit: number = 10): Promise<User[]> {
    const response = await api.get<User[]>(`/users/leaderboard?limit=${limit}`);
    return response.data;
  }
};

export default userService;
