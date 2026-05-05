import { axiosInstance } from './client';
import { type DailyDashboardDto, type WeeklyDashboardDto } from '@/types/dashboard';

export const dashboardApi = {
  getDaily: (date?: string) => 
    axiosInstance.get<DailyDashboardDto>('/dashboard/daily', { params: { date } }).then(res => res.data),
  
  getWeekly: (weekStart?: string) => 
    axiosInstance.get<WeeklyDashboardDto>('/dashboard/weekly', { params: { weekStart } }).then(res => res.data),
};
