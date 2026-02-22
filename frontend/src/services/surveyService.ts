import api from './api';
import { Survey, SurveyCreateRequest, Page, PageRequest } from '../types';

export const surveyService = {
  async getSurveys(params: PageRequest = {}): Promise<Page<Survey>> {
    const searchParams = new URLSearchParams();
    if (params.page !== undefined) searchParams.append('page', params.page.toString());
    if (params.size !== undefined) searchParams.append('size', params.size.toString());

    const response = await api.get<Page<Survey>>(`/surveys?${searchParams.toString()}`);
    return response.data;
  },

  async getSurvey(id: number): Promise<Survey> {
    const response = await api.get<Survey>(`/surveys/${id}`);
    return response.data;
  },

  async createSurvey(data: SurveyCreateRequest): Promise<Survey> {
    const response = await api.post<Survey>('/surveys', data);
    return response.data;
  },

  async vote(surveyId: number, optionIds: number[]): Promise<Survey> {
    const response = await api.post<Survey>(`/surveys/${surveyId}/vote`, { optionIds });
    return response.data;
  },

  async deleteSurvey(id: number): Promise<void> {
    await api.delete(`/surveys/${id}`);
  },

  async getActiveSurveys(): Promise<Survey[]> {
    const response = await api.get<Survey[]>('/surveys/active');
    return response.data;
  },
};
