import { axiosInstance } from './client';
import { type TimeSlotDto } from '@/types/booking';

export const availabilityApi = {
  getSlots: (date: string, serviceId: number) =>
    axiosInstance
      .get<TimeSlotDto[]>('/availability', { params: { date, serviceId } })
      .then((res) => res.data),
};
