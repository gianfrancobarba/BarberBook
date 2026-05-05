import { axiosInstance } from './client';
import { type ServiceResponseDto, type CreateServiceRequestDto, type UpdateServiceRequestDto } from '@/types/service';

export const servicesApi = {
  getAll: () => 
    axiosInstance.get<ServiceResponseDto[]>('/services').then(res => res.data),
  
  create: (data: CreateServiceRequestDto) => 
    axiosInstance.post<ServiceResponseDto>('/services', data).then(res => res.data),
  
  update: (id: number, data: UpdateServiceRequestDto) => 
    axiosInstance.patch<ServiceResponseDto>(`/services/${id}`, data).then(res => res.data),
  
  delete: (id: number) => 
    axiosInstance.delete(`/services/${id}`),
};
