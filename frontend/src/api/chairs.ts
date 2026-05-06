import { axiosInstance } from './client';
import { type ChairResponseDto, type CreateChairRequestDto, type UpdateChairRequestDto } from '@/types/chair';

export const chairsApi = {
  getAll: () => 
    axiosInstance.get<ChairResponseDto[]>('/chairs').then(res => res.data),
  
  create: (data: CreateChairRequestDto) => 
    axiosInstance.post<ChairResponseDto>('/chairs', data).then(res => res.data),
  
  update: (id: number, data: UpdateChairRequestDto) => 
    axiosInstance.patch<ChairResponseDto>(`/chairs/${id}`, data).then(res => res.data),
  
  delete: (id: number) => 
    axiosInstance.delete(`/chairs/${id}`),
};
