import { axiosInstance } from './client';
import { type NotificationResponseDto } from '@/types/notification';

export const notificationsApi = {
  getAll: () => 
    axiosInstance.get<NotificationResponseDto[]>('/notifications').then(res => res.data),
  
  markAsRead: (id: number) => 
    axiosInstance.patch(`/notifications/${id}/read`),
  
  markAllAsRead: () => 
    axiosInstance.patch('/notifications/read-all'),
};
