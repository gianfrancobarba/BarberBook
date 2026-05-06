import { useEffect, useState } from 'react';
import axios from 'axios';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { RouterProvider } from 'react-router-dom';
import { router } from './router';
import { Toaster } from "@/components/ui/sonner";
import { useSSE } from '@/hooks/useSSE';
import { ErrorBoundary } from '@/components/common/ErrorBoundary';
import { useAuthStore } from '@/stores/authStore';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: 1,
      staleTime: 30 * 1000,
      refetchOnWindowFocus: false,
    },
  },
});

/**
 * Componente di inizializzazione sessione.
 * Se `user` è nello store (persistito da localStorage) ma `accessToken` è null
 * (non persistito, in-memory only), tenta il refresh prima di rendere l'app.
 * Questo evita il 401/403 alla prima richiesta autenticata dopo un reload.
 */
function SessionInitializer({ children }: { children: React.ReactNode }) {
  const { user, accessToken, setAccessToken, clearSession } = useAuthStore();
  const [ready, setReady] = useState(false);

  useEffect(() => {
    // Se l'utente è nello store ma non abbiamo il token in memoria, proviamo il refresh
    if (user && !accessToken) {
      axios
        .post('/api/auth/refresh', {}, { withCredentials: true })
        .then(({ data }) => {
          setAccessToken(data.accessToken);
        })
        .catch(() => {
          // Cookie scaduto o invalido: puliamo la sessione
          clearSession();
        })
        .finally(() => {
          setReady(true);
        });
    } else {
      setReady(true);
    }
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  // Blocca il rendering finché il refresh non è completato (evita flash di stato non autenticato)
  if (!ready) return null;

  return <>{children}</>;
}

function AppContent() {
  // Attiva SSE globalmente
  useSSE();

  return (
    <>
      <RouterProvider router={router} />
      <Toaster position="top-right" richColors />
    </>
  );
}

export function App() {
  return (
    <ErrorBoundary>
      <QueryClientProvider client={queryClient}>
        <SessionInitializer>
          <AppContent />
        </SessionInitializer>
      </QueryClientProvider>
    </ErrorBoundary>
  )
}

export default App
