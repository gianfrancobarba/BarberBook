import { useQuery } from '@tanstack/react-query';
import { availabilityApi } from '@/api/availability';

export function useAvailableSlots(date: string | null, serviceId: number | null) {
  return useQuery({
    queryKey: ['availability', date, serviceId],
    queryFn: () => availabilityApi.getSlots(date!, serviceId!),
    enabled: !!date && !!serviceId,
    staleTime: 60 * 1000,
  });
}
