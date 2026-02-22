import axios, { AxiosError, InternalAxiosRequestConfig } from 'axios';
import { ApiError } from '../types';

const API_BASE_URL = '/api';

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request-Interceptor zum Hinzufügen von Auth-Token
api.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = localStorage.getItem('ideaboard_token');
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Response-Interceptor für Fehlerbehandlung und Token-Aktualisierung
api.interceptors.response.use(
  (response) => response,
  async (error: AxiosError<ApiError>) => {
    const originalRequest = error.config as InternalAxiosRequestConfig & { _retry?: boolean };

    // 401 Unauthorized behandeln - versuchen Token zu aktualisieren
    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;

      const refreshToken = localStorage.getItem('ideaboard_refresh_token');
      if (refreshToken) {
        try {
          const response = await axios.post(`${API_BASE_URL}/auth/refresh`, {
            refreshToken,
          });

          const { token } = response.data;
          localStorage.setItem('ideaboard_token', token);

          if (originalRequest.headers) {
            originalRequest.headers.Authorization = `Bearer ${token}`;
          }
          return api(originalRequest);
        } catch {
          // Aktualisierung fehlgeschlagen, Token löschen und zur Anmeldung umleiten
          localStorage.removeItem('ideaboard_token');
          localStorage.removeItem('ideaboard_refresh_token');
          localStorage.removeItem('ideaboard_user');
          window.location.href = '/login';
        }
      }
    }

    // Fehlerantwort transformieren
    const apiError: ApiError = {
      status: error.response?.status || 500,
      message: error.response?.data?.message || 'Ein unerwarteter Fehler ist aufgetreten',
      timestamp: new Date().toISOString(),
      path: error.config?.url,
      errors: error.response?.data?.errors,
    };

    return Promise.reject(apiError);
  }
);

export default api;
