import { useEffect } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { useAuthStore } from '@/stores/authStore';
import { useNotificationStore } from '@/stores/notificationStore';
import { toast } from 'sonner';

const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api';

export function useSSE() {
  const queryClient = useQueryClient();
  const { accessToken, isAuthenticated } = useAuthStore();
  const { setUnreadCount } = useNotificationStore();

  useEffect(() => {
    if (!isAuthenticated() || !accessToken) return;

    // Connessione SSE con token in query param (gestito dal backend)
    const eventSource = new EventSource(`${API_BASE_URL}/notifications/stream?token=${accessToken}`);

    eventSource.addEventListener('notification', (event) => {
      try {
        const data = JSON.parse(event.data);
        
        // 1. Aggiorna contatore notifiche
        if (data.unreadCount !== undefined) {
          setUnreadCount(data.unreadCount);
        }

        // 2. Notifica visiva (Toast)
        if (data.message) {
          toast(data.title || 'Nuova Notifica', {
            description: data.message,
          });
        }

        // 3. Invalida cache in base al tipo di evento
        if (data.type === 'BOOKING_NEW' || data.type === 'BOOKING_STATUS_CHANGED') {
          queryClient.invalidateQueries({ queryKey: ['dashboard'] });
          queryClient.invalidateQueries({ queryKey: ['bookings'] });
        }
        
        // Invalida sempre l'elenco notifiche
        queryClient.invalidateQueries({ queryKey: ['notifications'] });

      } catch (error) {
        console.error('Error parsing SSE notification:', error);
      }
    });

    eventSource.addEventListener('ping', () => {
      // Keep-alive gestito
    });

    eventSource.onerror = (error) => {
      console.error('SSE Connection Error:', error);
      eventSource.close();
    };

    return () => {
      eventSource.close();
    };
  }, [accessToken, isAuthenticated, queryClient, setUnreadCount]);
}
