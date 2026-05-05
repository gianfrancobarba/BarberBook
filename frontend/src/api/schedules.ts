import { axiosInstance } from './client';

export const schedulesApi = {
  getAllSchedules: () =>
    axiosInstance.get('/schedules').then(res => res.data),

  getChairSchedules: (chairId: number) => 
    axiosInstance.get(`/schedules/chairs/${chairId}`).then(res => res.data),
  
  addSchedule: (data: any) => 
    axiosInstance.post('/schedules', data).then(res => res.data),
  
  deleteSchedule: (id: number) => 
    axiosInstance.delete(`/schedules/${id}`),
};
