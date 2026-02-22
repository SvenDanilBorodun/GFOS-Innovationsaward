import api from './api';
import {
  Idea,
  IdeaCreateRequest,
  IdeaUpdateRequest,
  Page,
  IdeaFilter,
  Comment,
  CommentCreateRequest,
  LikeStatus,
  ChecklistItem,
  ChecklistToggleResponse
} from '../types';

export const ideaService = {
  // Ideen CRUD
  async getIdeas(filter: IdeaFilter = {}): Promise<Page<Idea>> {
    const params = new URLSearchParams();
    if (filter.page !== undefined) params.append('page', filter.page.toString());
    if (filter.size !== undefined) params.append('size', filter.size.toString());
    if (filter.sort) params.append('sort', filter.sort);
    if (filter.direction) params.append('direction', filter.direction);
    if (filter.category) params.append('category', filter.category);
    if (filter.status) params.append('status', filter.status);
    if (filter.authorId) params.append('authorId', filter.authorId.toString());
    if (filter.search) params.append('search', filter.search);
    if (filter.tags?.length) params.append('tags', filter.tags.join(','));

    const response = await api.get<Page<Idea>>(`/ideas?${params.toString()}`);
    return response.data;
  },

  async getIdea(id: number): Promise<Idea> {
    const response = await api.get<Idea>(`/ideas/${id}`);
    return response.data;
  },

  async createIdea(data: IdeaCreateRequest): Promise<Idea> {
    const response = await api.post<Idea>('/ideas', data);
    return response.data;
  },

  async updateIdea(id: number, data: IdeaUpdateRequest): Promise<Idea> {
    const response = await api.put<Idea>(`/ideas/${id}`, data);
    return response.data;
  },

  async deleteIdea(id: number): Promise<void> {
    await api.delete(`/ideas/${id}`);
  },

  // Dateianhänge
  async uploadFile(ideaId: number, file: File): Promise<void> {
    const formData = new FormData();
    formData.append('file', file);
    await api.post(`/ideas/${ideaId}/files`, formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
  },

  async deleteFile(ideaId: number, fileId: number): Promise<void> {
    await api.delete(`/ideas/${ideaId}/files/${fileId}`);
  },

  async downloadFile(ideaId: number, fileId: number): Promise<Blob> {
    const response = await api.get(`/ideas/${ideaId}/files/${fileId}`, {
      responseType: 'blob',
    });
    return response.data;
  },

  // Likes
  async likeIdea(ideaId: number): Promise<void> {
    await api.post(`/ideas/${ideaId}/like`);
  },

  async unlikeIdea(ideaId: number): Promise<void> {
    await api.delete(`/ideas/${ideaId}/like`);
  },

  async getLikeStatus(): Promise<LikeStatus> {
    const response = await api.get<LikeStatus>('/users/me/likes/remaining');
    return response.data;
  },

  // Kommentare
  async getComments(ideaId: number): Promise<Comment[]> {
    const response = await api.get<Comment[]>(`/ideas/${ideaId}/comments`);
    return response.data;
  },

  async createComment(ideaId: number, data: CommentCreateRequest): Promise<Comment> {
    const response = await api.post<Comment>(`/ideas/${ideaId}/comments`, data);
    return response.data;
  },

  async deleteComment(commentId: number): Promise<void> {
    await api.delete(`/comments/${commentId}`);
  },

  // Reaktionen
  async addReaction(commentId: number, emoji: string): Promise<void> {
    await api.post(`/comments/${commentId}/reactions`, { emoji });
  },

  async removeReaction(commentId: number, emoji: string): Promise<void> {
    await api.delete(`/comments/${commentId}/reactions/${emoji}`);
  },

  // Status (Nur PM/Admin)
  async updateStatus(ideaId: number, status: string, progressPercentage?: number): Promise<Idea> {
    const response = await api.put<Idea>(`/ideas/${ideaId}/status`, {
      status,
      progressPercentage,
    });
    return response.data;
  },

  // Kategorien
  async getCategories(): Promise<string[]> {
    const response = await api.get<string[]>('/ideas/categories');
    return response.data;
  },

  // Tags
  async getPopularTags(limit: number = 20): Promise<string[]> {
    const response = await api.get<string[]>(`/ideas/tags/popular?limit=${limit}`);
    return response.data;
  },

  // Checkliste
  async getChecklist(ideaId: number): Promise<ChecklistItem[]> {
    const response = await api.get<ChecklistItem[]>(`/ideas/${ideaId}/checklist`);
    return response.data;
  },

  async createChecklistItem(ideaId: number, title: string): Promise<ChecklistItem> {
    const response = await api.post<ChecklistItem>(`/ideas/${ideaId}/checklist`, { title });
    return response.data;
  },

  async toggleChecklistItem(ideaId: number, itemId: number): Promise<ChecklistToggleResponse> {
    const response = await api.patch<ChecklistToggleResponse>(`/ideas/${ideaId}/checklist/${itemId}/toggle`);
    return response.data;
  },

  async updateChecklistItem(ideaId: number, itemId: number, title: string): Promise<ChecklistItem> {
    const response = await api.put<ChecklistItem>(`/ideas/${ideaId}/checklist/${itemId}`, { title });
    return response.data;
  },

  async deleteChecklistItem(ideaId: number, itemId: number): Promise<void> {
    await api.delete(`/ideas/${ideaId}/checklist/${itemId}`);
  },
};
