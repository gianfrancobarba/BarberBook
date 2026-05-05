import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { notificationsApi } from '@/api/notifications';
import { useNotificationStore } from '@/stores/notificationStore';

export function useNotifications() {
  const setUnreadCount = useNotificationStore((state) => state.setUnreadCount);
  return useQuery({
    queryKey: ['notifications'],
    queryFn: async () => {
      const data = await notificationsApi.getAll();
      const unread = data.filter(n => !n.letta).length;
      setUnreadCount(unread);
      return data;
    },
  });
}

export function useMarkAsRead() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: notificationsApi.markAsRead,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['notifications'] });
    },
  });
}

export function useMarkAllAsRead() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: notificationsApi.markAllAsRead,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['notifications'] });
    },
  });
}
