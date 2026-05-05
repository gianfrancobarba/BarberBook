import { axiosInstance } from './client';
import { 
  type BookingResponseDto, 
  type CreateBookingRequestDto, 
  type GuestBookingRequestDto,
  type DirectBookingRequestDto,
  type BookingStatus
} from '@/types/booking';

export const bookingsApi = {
  create: (data: CreateBookingRequestDto) =>
    axiosInstance.post<BookingResponseDto>('/bookings', data).then(res => res.data),

  createGuest: (data: GuestBookingRequestDto) =>
    axiosInstance.post<BookingResponseDto>('/bookings/guest', data).then(res => res.data),

  createDirect: (data: DirectBookingRequestDto) =>
    axiosInstance.post<BookingResponseDto>('/bookings/direct', data).then(res => res.data),

  accept: (id: number) =>
    axiosInstance.patch<BookingResponseDto>(`/bookings/${id}/accept`).then(res => res.data),

  reject: (id: number) =>
    axiosInstance.patch<BookingResponseDto>(`/bookings/${id}/reject`).then(res => res.data),

  cancel: (id: number, reason: string) =>
    axiosInstance.patch<BookingResponseDto>(`/bookings/${id}/cancel`, { reason }).then(res => res.data),

  rebook: (id: number) =>
    axiosInstance.post<BookingResponseDto>(`/bookings/${id}/rebook`).then(res => res.data),

  getPending: () =>
    axiosInstance.get<BookingResponseDto[]>('/bookings/pending').then(res => res.data),

  getClientBookings: (status?: BookingStatus) =>
    axiosInstance.get<BookingResponseDto[]>('/client/bookings', { params: { status } }).then(res => res.data),

  getUpcoming: () =>
    axiosInstance.get<BookingResponseDto[]>('/client/bookings/upcoming').then(res => res.data),

  update: (id: number, data: any) =>
    axiosInstance.patch<BookingResponseDto>(`/bookings/${id}`, data).then(res => res.data),

  delete: (id: number) =>
    axiosInstance.delete(`/bookings/${id}`),
};
