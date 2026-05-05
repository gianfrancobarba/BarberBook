import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { bookingsApi } from '@/api/bookings';
import { type BookingStatus } from '@/types/booking';

export function useCreateBooking() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: bookingsApi.create,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['availability'] });
      queryClient.invalidateQueries({ queryKey: ['bookings'] });
    },
  });
}

export function useGuestBooking() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: bookingsApi.createGuest,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['availability'] });
    },
  });
}

export function useAcceptBooking() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: bookingsApi.accept,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['pending-bookings'] });
      queryClient.invalidateQueries({ queryKey: ['dashboard'] });
    },
  });
}

export function useRejectBooking() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: bookingsApi.reject,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['pending-bookings'] });
    },
  });
}

export function useCancelBooking() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, reason }: { id: number; reason: string }) => bookingsApi.cancel(id, reason),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['bookings'] });
      queryClient.invalidateQueries({ queryKey: ['dashboard'] });
    },
  });
}

export function usePendingBookings() {
  return useQuery({
    queryKey: ['pending-bookings'],
    queryFn: bookingsApi.getPending,
    refetchInterval: 30 * 1000, // Refresh ogni 30s per il barbiere
  });
}

export function useMyBookings(status?: BookingStatus) {
  return useQuery({
    queryKey: ['bookings', 'client', status],
    queryFn: () => bookingsApi.getClientBookings(status),
  });
}

export function useUpcomingBookings() {
  return useQuery({
    queryKey: ['bookings', 'upcoming'],
    queryFn: bookingsApi.getUpcoming,
  });
}

export function useRebook() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: bookingsApi.rebook,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['bookings'] });
    },
  });
}
