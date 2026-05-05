import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { chairsApi } from '@/api/chairs';

export function useChairs() {
  return useQuery({
    queryKey: ['chairs'],
    queryFn: chairsApi.getAll,
  });
}

export function useCreateChair() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: chairsApi.create,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['chairs'] });
    },
  });
}

export function useUpdateChair() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: any }) => chairsApi.update(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['chairs'] });
    },
  });
}

export function useDeleteChair() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: chairsApi.delete,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['chairs'] });
    },
  });
}
