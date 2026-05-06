import { useQuery } from '@tanstack/react-query';
import { availabilityApi } from '@/api/availability';
import { type BookableSlot } from '@/types/booking';

export function useAvailableSlots(date: string | null, serviceId: number | null) {
  return useQuery<BookableSlot[]>({
    queryKey: ['availability', date, serviceId],
    queryFn: async () => {
      const chairs = await availabilityApi.getSlots(date!, serviceId!);
      // Flatten: per ogni poltrona, per ogni slot, costruisce ISO datetime completo
      return chairs.flatMap((chair) =>
        chair.availableSlots.map((slot) => ({
          chairId: chair.chairId,
          chairName: chair.chairName,
          startTime: `${date}T${slot.start}:00`,
          endTime:   `${date}T${slot.end}:00`,
        }))
      );
    },
    enabled: !!date && !!serviceId,
    staleTime: 60 * 1000,
  });
}
