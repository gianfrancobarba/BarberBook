import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { useAuthStore } from '@/stores/authStore';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: 1,
      staleTime: 30 * 1000, // 30 secondi
      refetchOnWindowFocus: false,
    },
  },
});

function App() {
  const { user, isAuthenticated } = useAuthStore();

  return (
    <QueryClientProvider client={queryClient}>
      <div className="flex min-h-screen items-center justify-center bg-zinc-950 text-white">
        <div className="text-center">
          <h1 className="text-4xl font-bold tracking-tight text-orange-500">BarberBook</h1>
          <p className="mt-2 text-zinc-400">
            {isAuthenticated() 
              ? `Bentornato, ${user?.nome}!` 
              : "Infrastruttura API & State Management Pronta"}
          </p>
          <div className="mt-4 text-xs text-zinc-500">
            Axios + Zustand + TanStack Query configurati
          </div>
        </div>
      </div>
    </QueryClientProvider>
  )
}

export default App
