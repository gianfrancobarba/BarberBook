import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import { type UserResponseDto } from '@/types/auth';

interface AuthState {
  accessToken: string | null;
  user: UserResponseDto | null;
  setAccessToken: (token: string | null) => void;
  setUser: (user: UserResponseDto | null) => void;
  clearSession: () => void;
  isAuthenticated: () => boolean;
  isBarber: () => boolean;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set, get) => ({
      accessToken: null,
      user: null,
      setAccessToken: (token) => set({ accessToken: token }),
      setUser: (user) => set({ user }),
      clearSession: () => set({ accessToken: null, user: null }),
      isAuthenticated: () => !!get().accessToken,
      isBarber: () => get().user?.ruolo === 'BARBER',
    }),
    {
      name: 'barber-auth-storage',
    }
  )
);
