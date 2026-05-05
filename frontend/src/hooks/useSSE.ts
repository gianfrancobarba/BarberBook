import { useEffect } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { useAuthStore } from '@/stores/authStore';
import { useNotificationStore } from '@/stores/notificationStore';
import { toast } from 'sonner';

export function useSSE() {
  const queryClient = useQueryClient();
  const { accessToken, isAuthenticated } = useAuthStore();
  const { setUnreadCount } = useNotificationStore();

  useEffect(() => {
    if (!isAuthenticated() || !accessToken) return;

    // EventSource non supporta header custom: il token viene passato come query param.
    // Il backend (JwtAuthFilter) lo legge da ?token= per le connessioni SSE.
    const eventSource = new EventSource(`/api/notifications/stream?token=${accessToken}`);

    eventSource.addEventListener('notification', (event) => {
      try {
        const data = JSON.parse(event.data);

        // I nomi dei campi corrispondono a NotificationPushDto del backend
        if (data.messaggio) {
          toast(data.titolo || 'Nuova Notifica', {
            description: data.messaggio,
          });
        }

        // Invalida cache in base al tipo di notifica (valori da NotificationType)
        const bookingEventTypes = [
          'NUOVA_RICHIESTA',
          'PRENOTAZIONE_ACCETTATA',
          'PRENOTAZIONE_RIFIUTATA',
          'ANNULLAMENTO_DA_CLIENTE',
          'ANNULLAMENTO_DA_BARBIERE',
        ];
        if (bookingEventTypes.includes(data.tipo)) {
          queryClient.invalidateQueries({ queryKey: ['dashboard'] });
          queryClient.invalidateQueries({ queryKey: ['bookings'] });
          queryClient.invalidateQueries({ queryKey: ['pending-bookings'] });
        }

        queryClient.invalidateQueries({ queryKey: ['notifications'] });

        // Aggiorna il contatore non lette recuperandolo dal server
        // (NotificationPushDto non include unreadCount — viene refreshato via query)
        queryClient.invalidateQueries({ queryKey: ['notifications-unread'] });

      } catch (error) {
        console.error('Error parsing SSE notification:', error);
      }
    });

    eventSource.addEventListener('connected', () => {
      // Heartbeat iniziale ricevuto — connessione SSE attiva
    });

    eventSource.onerror = () => {
      eventSource.close();
    };

    return () => {
      eventSource.close();
    };
  }, [accessToken, isAuthenticated, queryClient, setUnreadCount]);
}
