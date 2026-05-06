import { axiosInstance } from './client';
import { type AvailabilityResponseDto } from '@/types/booking';

export const availabilityApi = {
  getSlots: (date: string, serviceId: number) =>
    axiosInstance
      .get<AvailabilityResponseDto[]>('/availability', { params: { date, serviceId } })
      .then((res) => res.data),
};
