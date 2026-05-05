import { axiosInstance } from './client';
import { type AuthResponseDto, type UserResponseDto } from '@/types/auth';

export const authApi = {
  login: (credentials: any) => 
    axiosInstance.post<AuthResponseDto>('/auth/login', credentials).then(res => res.data),
  
  register: (data: any) => 
    axiosInstance.post<AuthResponseDto>('/auth/register', data).then(res => res.data),
  
  logout: () => 
    axiosInstance.post('/auth/logout'),
  
  me: () => 
    axiosInstance.get<UserResponseDto>('/users/me').then(res => res.data),

  forgotPassword: (email: string) =>
    axiosInstance.post('/auth/forgot-password', { email }),

  resetPassword: (data: any) =>
    axiosInstance.post('/auth/reset-password', data),
};
