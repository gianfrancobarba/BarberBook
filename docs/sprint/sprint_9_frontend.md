# Sprint 9 — Frontend React
> **Stato**: ⬜ Non iniziato  
> **Dipende da**: Sprint 1-8 ✅ (tutte le API backend stabili)  
> **Obiettivo**: Realizzare l'interfaccia utente completa in React + TypeScript che consuma tutte le API backend validate. Il frontend è sviluppato con uno stile "API-first": ogni schermata è il consumatore di un contratto API già testato.

---

## Stack Tecnologico Frontend

| Tecnologia | Versione | Ruolo |
|------------|----------|-------|
| React | 18 | Framework UI |
| Vite | 6 | Build tool & Dev Server |
| TypeScript | 5 | Type safety |
| Tailwind CSS | v4 | Styling utility-first |
| shadcn/ui | latest | Componenti UI base |
| React Router | v7 | Client-side routing |
| TanStack Query | v5 | Server state management + cache |
| Zustand | ^4 | Client state (auth, notifiche) |
| Axios | ^1 | HTTP client con interceptors |
| React Hook Form | ^7 | Form management |
| Zod | ^3 | Validazione schema type-safe |
| date-fns | ^3 | Date formatting |
| Lucide React | latest | Icone SVG |
| native EventSource | — | SSE per notifiche real-time |
| MSW | ^2 | Mock Service Worker (sviluppo parallelo) |
| Vitest + RTL | latest | Unit e component testing |

---

## Indice Fasi

1. [Fase 9.1 — Scaffolding Vite + Configurazione](#fase-91--scaffolding-vite--configurazione)
2. [Fase 9.2 — Design System & Tailwind](#fase-92--design-system--tailwind)
3. [Fase 9.3 — Infrastruttura: Axios, Zustand, TanStack Query](#fase-93--infrastruttura-axios-zustand-tanstack-query)
4. [Fase 9.4 — Custom Hooks API](#fase-94--custom-hooks-api)
5. [Fase 9.5 — Routing & Layout](#fase-95--routing--layout)
6. [Fase 9.6 — Autenticazione (Login, Registrazione)](#fase-96--autenticazione-login-registrazione)
7. [Fase 9.7 — Flusso Prenotazione Cliente](#fase-97--flusso-prenotazione-cliente)
8. [Fase 9.8 — Area Cliente (Storico, Profilo)](#fase-98--area-cliente-storico-profilo)
9. [Fase 9.9 — Dashboard BAR (Giornaliera, Settimanale)](#fase-99--dashboard-bar-giornaliera-settimanale)
10. [Fase 9.10 — Gestione Risorse BAR (Servizi, Poltrone, Orari)](#fase-910--gestione-risorse-bar-servizi-poltrone-orari)
11. [Fase 9.11 — Notifiche SSE Real-time](#fase-911--notifiche-sse-real-time)
12. [Fase 9.12 — Test Frontend](#fase-912--test-frontend)
13. [Fase 9.13 — Responsive & Accessibilità](#fase-913--responsive--accessibilità)
14. [Fase 9.14 — Verifica Quality Gate](#fase-914--verifica-quality-gate)

---

## Fase 9.1 — Scaffolding Vite + Configurazione

**Obiettivo**: Creare il progetto frontend con Vite e configurare l'ambiente completo.

### Setup iniziale
```bash
# Da eseguire nella root del monorepo
npm create vite@latest frontend -- --template react-ts
cd frontend
npm install
```

### Dipendenze da installare
```bash
# Core
npm install react-router-dom @tanstack/react-query zustand axios

# Form & Validazione
npm install react-hook-form zod @hookform/resolvers

# UI
npm install tailwindcss@latest @tailwindcss/vite
npx shadcn@latest init

# Utility
npm install date-fns lucide-react clsx tailwind-merge

# Dev
npm install -D vitest @testing-library/react @testing-library/jest-dom
npm install -D msw@latest
npm install -D @types/node
```

### Struttura directory frontend
```
frontend/src/
├── api/                  ← client Axios e funzioni API per endpoint
│   ├── auth.ts
│   ├── bookings.ts
│   ├── availability.ts
│   ├── services.ts
│   ├── chairs.ts
│   ├── dashboard.ts
│   ├── notifications.ts
│   └── client.ts        ← axiosInstance + interceptors
├── components/
│   ├── ui/              ← shadcn/ui (generati, non modificare)
│   ├── layout/          ← Layout, Navbar, Sidebar
│   ├── booking/         ← componenti specifici prenotazione
│   ├── dashboard/       ← componenti specifici dashboard BAR
│   └── common/          ← Badge, Spinner, ErrorBoundary, EmptyState
├── hooks/               ← Custom Hooks (TanStack Query)
├── pages/               ← Route-level components (Container)
├── stores/              ← Zustand stores
├── types/               ← TypeScript interfaces
├── lib/                 ← utility, helpers
├── mocks/               ← MSW handlers (sviluppo/test)
└── App.tsx
```

### `vite.config.ts` con proxy
```typescript
export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
});
```

### Attività
- [ ] Eseguire `npm create vite@latest frontend`
- [ ] Installare tutte le dipendenze
- [ ] Configurare `vite.config.ts` con proxy backend
- [ ] Inizializzare shadcn/ui (`npx shadcn@latest init`)
- [ ] Creare la struttura directory completa
- [ ] Configurare Vitest in `vite.config.ts`
- [ ] Aggiornare `docker-compose.yml` per servire il frontend

---

## Fase 9.2 — Design System & Tailwind

**Obiettivo**: Definire il design system: palette colori, tipografia, spacing. Tutto il resto del frontend si basa su questi token.

### `tailwind.config.js` — Tema personalizzato
```javascript
/** @type {import('tailwindcss').Config} */
export default {
  content: ['./src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      colors: {
        barber: {
          50:  '#fef7f0',
          100: '#feecda',
          200: '#fcd5b4',
          300: '#f9b57d',
          400: '#f58b43',
          500: '#f2691a',  // accent principale
          600: '#e35012',
          700: '#bc3b11',
          800: '#963016',
          900: '#782a15',
          950: '#411209',
        },
        neutral: {
          // scala grigio neutro già in Tailwind — utilizzare directamente
        }
      },
      fontFamily: {
        sans: ['Inter', 'system-ui', 'sans-serif'],
        heading: ['Outfit', 'sans-serif'],
      }
    }
  }
}
```

### `index.css` — Import Google Fonts + variabili CSS
```css
@import url('https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600&family=Outfit:wght@600;700;800&display=swap');
@import 'tailwindcss';

:root {
  --barber-primary: #f2691a;
  --radius: 0.625rem;
}
```

### Componenti base da creare in `components/common/`
```
Spinner.tsx         ← loading indicator
Badge.tsx           ← Badge stato prenotazione con colori semantici
EmptyState.tsx      ← illustrazione + testo per liste vuote
ErrorBoundary.tsx   ← cattura errori React, mostra UI fallback
ConfirmDialog.tsx   ← dialog di conferma per azioni distruttive
```

### Badge colori stato prenotazione
| Stato | Colore | Badge UI |
|-------|--------|---------|
| IN_ATTESA | Giallo/amber | `bg-amber-100 text-amber-800` |
| ACCETTATA | Verde | `bg-green-100 text-green-800` |
| RIFIUTATA | Rosso | `bg-red-100 text-red-800` |
| ANNULLATA | Grigio | `bg-gray-100 text-gray-500` |
| PASSATA | Slate | `bg-slate-100 text-slate-500` |

### Attività
- [ ] Configurare `tailwind.config.js` con palette colori salone
- [ ] Aggiornare `index.css` con import font + variabili CSS
- [ ] Creare componenti comuni (`Spinner`, `Badge`, `EmptyState`, `ErrorBoundary`)

---

## Fase 9.3 — Infrastruttura: Axios, Zustand, TanStack Query

**Obiettivo**: Configurare l'infrastruttura di comunicazione con il backend e la gestione dello stato.

### `api/client.ts` — Axios con interceptors JWT
```typescript
import axios, { AxiosError, type InternalAxiosRequestConfig } from 'axios';

export const axiosInstance = axios.create({
  baseURL: '/api',
  withCredentials: true,  // per inviare cookie HttpOnly
});

// Inietta Access Token su ogni request
axiosInstance.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = useAuthStore.getState().accessToken;
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  }
);

// Gestione token scaduto: refresh automatico
let isRefreshing = false;
axiosInstance.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const originalRequest = error.config as InternalAxiosRequestConfig & { _retry?: boolean };

    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;

      if (!isRefreshing) {
        isRefreshing = true;
        try {
          const { data } = await axios.post('/api/auth/refresh', {}, { withCredentials: true });
          useAuthStore.getState().setAccessToken(data.accessToken);
          isRefreshing = false;
          return axiosInstance(originalRequest);
        } catch (refreshError) {
          isRefreshing = false;
          useAuthStore.getState().clearSession();
          window.location.href = '/login';
          return Promise.reject(refreshError);
        }
      }
    }

    return Promise.reject(error);
  }
);
```

### `stores/authStore.ts` — Zustand per sessione
```typescript
interface AuthState {
  accessToken: string | null;
  user: UserResponseDto | null;
  setAccessToken: (token: string) => void;
  setUser: (user: UserResponseDto) => void;
  clearSession: () => void;
  isAuthenticated: () => boolean;
  isBarber: () => boolean;
}

export const useAuthStore = create<AuthState>((set, get) => ({
  accessToken: null,
  user: null,
  setAccessToken: (token) => set({ accessToken: token }),
  setUser: (user) => set({ user }),
  clearSession: () => set({ accessToken: null, user: null }),
  isAuthenticated: () => get().accessToken !== null,
  isBarber: () => get().user?.ruolo === 'BARBER',
}));
```

### `App.tsx` — TanStack QueryClient
```typescript
const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: 1,
      staleTime: 30 * 1000,  // 30 secondi
    },
  },
});

export function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <RouterProvider router={router} />
    </QueryClientProvider>
  );
}
```

### Attività
- [ ] Creare `api/client.ts` con Axios + interceptors
- [ ] Creare `stores/authStore.ts` con Zustand
- [ ] Configurare `App.tsx` con `QueryClientProvider`
- [ ] Creare `stores/notificationStore.ts` per badge notifiche non lette

---

## Fase 9.4 — Custom Hooks API

**Obiettivo**: Ogni feature ha il proprio hook TanStack Query che incapsula la logica di fetch, cache e mutazioni.

### Hooks da creare in `hooks/`
```typescript
// hooks/useAuth.ts
export function useLogin()      // mutation POST /auth/login
export function useLogout()     // mutation POST /auth/logout
export function useRegister()   // mutation POST /auth/register
export function useCurrentUser() // query GET /users/me

// hooks/useAvailability.ts
export function useAvailableSlots(date: string, serviceId: number)
// query GET /availability?date=...&serviceId=...

// hooks/useBookings.ts
export function useCreateBooking()       // mutation POST /bookings
export function useGuestBooking()        // mutation POST /bookings/guest
export function useAcceptBooking()       // mutation PATCH /bookings/{id}/accept
export function useRejectBooking()       // mutation PATCH /bookings/{id}/reject
export function useCancelBooking()       // mutation PATCH /bookings/{id}/cancel
export function useCreateDirectBooking() // mutation POST /bookings/direct
export function useUpdateBooking()       // mutation PATCH /bookings/{id}
export function useDeleteBooking()       // mutation DELETE /bookings/{id}
export function usePendingBookings()     // query GET /bookings/pending
export function useMyBookings(status?)   // query GET /client/bookings[?status=]
export function useUpcomingBookings()    // query GET /client/bookings/upcoming
export function useRebook()             // mutation POST /bookings/{id}/rebook

// hooks/useServices.ts
export function useServices()      // query GET /services
export function useCreateService() // mutation POST /services
export function useUpdateService() // mutation PATCH /services/{id}
export function useDeleteService() // mutation DELETE /services/{id}

// hooks/useChairs.ts
export function useChairs()       // query GET /chairs
export function useCreateChair()  // mutation POST /chairs
export function useRenameChair()  // mutation PATCH /chairs/{id}
export function useDeleteChair()  // mutation DELETE /chairs/{id}

// hooks/useDashboard.ts
export function useDailyDashboard(date?: string)     // query GET /dashboard/daily
export function useWeeklyDashboard(weekStart?: string) // query GET /dashboard/weekly

// hooks/useNotifications.ts
export function useNotifications()    // query GET /notifications
export function useMarkAsRead()       // mutation PATCH /notifications/{id}/read
export function useMarkAllAsRead()    // mutation PATCH /notifications/read-all

// hooks/useSchedules.ts
export function useChairSchedule(chairId: number) // query GET /schedules/chairs/{id}
export function useAddSchedule()   // mutation POST /schedules
export function useDeleteSchedule() // mutation DELETE /schedules/{id}
```

### Pattern tipo per ogni hook
```typescript
// Esempio: useAvailableSlots
export function useAvailableSlots(date: string | null, serviceId: number | null) {
  return useQuery({
    queryKey: ['availability', date, serviceId],
    queryFn: () => availabilityApi.getSlots(date!, serviceId!),
    enabled: !!date && !!serviceId,  // fetch solo se entrambi valorizzati
    staleTime: 60 * 1000,  // 60 secondi (gli slot cambiano raramente)
  });
}
```

### Invalidazione cache post-mutazione
```typescript
// Esempio: useCreateBooking
export function useCreateBooking() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: bookingsApi.create,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['availability'] });
      queryClient.invalidateQueries({ queryKey: ['pending-bookings'] });
    },
  });
}
```

### Attività
- [ ] Creare tutti i file API (`api/auth.ts`, `api/bookings.ts`, ecc.) con le funzioni Axios
- [ ] Creare tutti gli hooks con la corretta configurazione TanStack Query
- [ ] Configurare `queryKey` standard per ogni risorsa
- [ ] Configurare `invalidateQueries` sulle mutazioni

---

## Fase 9.5 — Routing & Layout

**Obiettivo**: Struttura di routing con protezione delle rotte per ruolo.

### `router.tsx` con React Router v7
```typescript
const router = createBrowserRouter([
  // Pubbliche
  { path: '/login', element: <LoginPage /> },
  { path: '/register', element: <RegisterPage /> },
  { path: '/forgot-password', element: <ForgotPasswordPage /> },
  { path: '/reset-password', element: <ResetPasswordPage /> },

  // Pubbliche: vetrina e prenotazione
  {
    path: '/',
    element: <PublicLayout />,
    children: [
      { index: true, element: <LandingPage /> },       // vetrina servizi
      { path: '/book', element: <BookingFlowPage /> }, // flusso multi-step
    ]
  },

  // Protette: solo CLIENT
  {
    path: '/my',
    element: <ProtectedRoute requiredRole="CLIENT" />,
    children: [
      { element: <ClientLayout />, children: [
        { index: true, element: <ClientHomepage /> },
        { path: 'bookings', element: <ClientBookingsPage /> },
        { path: 'profile', element: <ClientProfilePage /> },
      ]}
    ]
  },

  // Protette: solo BARBER
  {
    path: '/barber',
    element: <ProtectedRoute requiredRole="BARBER" />,
    children: [
      { element: <BarberLayout />, children: [
        { index: true, element: <DailyDashboardPage /> },
        { path: 'weekly', element: <WeeklyDashboardPage /> },
        { path: 'services', element: <ManageServicesPage /> },
        { path: 'chairs', element: <ManageChairsPage /> },
        { path: 'schedules', element: <ManageSchedulesPage /> },
      ]}
    ]
  },

  { path: '*', element: <NotFoundPage /> }
]);
```

### `ProtectedRoute.tsx`
```typescript
function ProtectedRoute({ requiredRole }: { requiredRole?: 'BARBER' | 'CLIENT' }) {
  const { isAuthenticated, user } = useAuthStore();

  if (!isAuthenticated()) return <Navigate to="/login" replace />;
  if (requiredRole && user?.ruolo !== requiredRole)
    return <Navigate to="/" replace />;

  return <Outlet />;
}
```

### Layout da creare
```
PublicLayout     ← Navbar pubblica con link prenotazione + login
ClientLayout     ← Sidebar cliente (Homepage, Prenotazioni, Profilo) + NotificationBell
BarberLayout     ← Sidebar BAR (Agenda, Settimanale, Servizi, Poltrone, Orari) + NotificationBell
```

### Attività
- [ ] Creare `router.tsx` con tutte le rotte
- [ ] Creare `ProtectedRoute.tsx` con protezione per ruolo
- [ ] Creare i 3 layout (`PublicLayout`, `ClientLayout`, `BarberLayout`)
- [ ] Creare `NotificationBell.tsx` con badge non lette

---

## Fase 9.6 — Autenticazione (Login, Registrazione)

**Obiettivo**: Schermate di accesso al sistema con validazione form completa.

### Pagine da creare

#### `LoginPage.tsx`
- Form: email + password
- Validazione: `email` formato valido, `password` non vuota
- Gestione errori: `401` → "Credenziali non valide"
- Redirect post-login: BAR → `/barber`, CLR → `/my`
- Link: "Hai dimenticato la password?" + "Registrati"

#### `RegisterPage.tsx`
- Form: nome, cognome, email, telefono (opzionale), password, conferma password
- Validazione Zod: password ≥ 8 char, email formato valido, conferma password uguale
- Gestione errori: `400` email duplicata → "Email già registrata"
- Redirect post-registrazione: `/my`

#### `ForgotPasswordPage.tsx`
- Form: solo email
- Messaggio generico post-submit (non rivela se email esiste)
- Link: "Torna al login"

#### `ResetPasswordPage.tsx`
- Legge `?token=...` dalla query string
- Form: nuova password + conferma
- Gestione errori: token scaduto/usato
- Redirect post-reset: `/login`

### Pattern Container/Presenter da rispettare
```
LoginPage.tsx (Container)
  └── LoginForm.tsx (Presenter)
        └── useLogin() per la mutazione
```

### Attività
- [ ] Creare `LoginPage` + `LoginForm`
- [ ] Creare `RegisterPage` + `RegisterForm` con schema Zod
- [ ] Creare `ForgotPasswordPage` + `ForgotPasswordForm`
- [ ] Creare `ResetPasswordPage` + `ResetPasswordForm`
- [ ] Testare redirect post-auth

---

## Fase 9.7 — Flusso Prenotazione Cliente

**Obiettivo**: Flusso multi-step guidato per la prenotazione — sia per CLR che per CLG.

### Architettura: Flusso Multi-Step
```
BookingFlowPage (Container — gestisce step corrente e dati accumulati)
  ├── Step 1: ServiceSelectionStep (scegliere servizio)
  ├── Step 2: DatePickerStep (scegliere data)
  ├── Step 3: SlotPickerStep (scegliere poltrona+orario disponibile)
  ├── Step 4: GuestDataStep (solo se CLG: inserire nome/cognome/telefono)
  └── Step 5: ConfirmationStep (riepilogo + conferma)
```

### State management del flusso
```typescript
// Zustand store per il flusso di prenotazione
interface BookingFlowState {
  step: number;
  selectedService: ServiceResponseDto | null;
  selectedDate: string | null;
  selectedChair: ChairResponseDto | null;
  selectedSlot: TimeSlotDto | null;
  guestData: GuestData | null;
  nextStep: () => void;
  prevStep: () => void;
  reset: () => void;
}
```

### Componenti chiave
```
ServiceCard.tsx       ← card servizio con nome, durata, prezzo
SlotGrid.tsx          ← griglia slot disponibili per poltrona
ChairTab.tsx          ← tab per selezionare tra più poltrone
GuestDataForm.tsx     ← form dati ospite (nome, cognome, telefono) con Zod
BookingConfirmCard.tsx ← riepilogo prenotazione prima della conferma
BookingSuccessPage.tsx ← conferma avvenuta + invito a registrarsi (CLG)
```

### Attività
- [ ] Creare `BookingFlowPage` con gestione step via Zustand
- [ ] Creare i 5 step del flusso come componenti separati
- [ ] Creare `SlotGrid.tsx` che consuma `useAvailableSlots`
- [ ] Gestire il caso CLG (mostrare Step 4 solo se non autenticato)
- [ ] Post-conferma CLG: mostrare invito a registrarsi (RF_CLG_2)

---

## Fase 9.8 — Area Cliente (Storico, Profilo)

**Obiettivo**: Il portale personale del CLR con homepage, storico filtrato e gestione profilo.

### Pagine da creare

#### `ClientHomepage.tsx`
- Benvenuto con nome cliente
- Card "Prossimi appuntamenti" (max 3 visualizzati)
- CTA: "Prenota ora!" → `/book`
- Badge notifiche non lette

#### `ClientBookingsPage.tsx`
- Lista prenotazioni con filtro per stato (tabbar o select)
- Per ogni prenotazione: card con nome servizio, data/ora, poltrona, stato
- Azioni contestuali: "Annulla" (solo su IN_ATTESA/ACCETTATA future), "Riprenota" (solo su PASSATE)
- Modal di annullamento con campo motivazione obbligatorio

#### `ClientProfilePage.tsx`
- Form modifica: nome, cognome, email, telefono
- Validazione inline con React Hook Form + Zod
- Feedback visivo al salvataggio (toast)

### Attività
- [ ] Creare `ClientHomepage` con prossimi appuntamenti
- [ ] Creare `ClientBookingsPage` con filtri e azioni
- [ ] Creare `CancelBookingModal` con campo motivazione obbligatorio
- [ ] Creare `ClientProfilePage` con form modifica profilo

---

## Fase 9.9 — Dashboard BAR (Giornaliera, Settimanale)

**Obiettivo**: Le viste operative dell'agenda del BAR.

### Pagine da creare

#### `DailyDashboardPage.tsx`
- DatePicker per navigazione giorno (default: oggi)
- Per ogni poltrona: lista prenotazioni con badge stato
- Card prenotazione: nome cliente, servizio, orario, durata
- Azioni inline: Accetta / Rifiuta (su IN_ATTESA), Cancella (su ACCETTATA)
- Bottone "Crea prenotazione diretta" → modal

#### `WeeklyDashboardPage.tsx`
- Vista calendario settimanale (7 giorni × N poltrone)
- Navigazione settimana precedente/successiva
- Ogni cella: lista prenotazioni del giorno per quella poltrona
- Click su cella → mostra dettaglio giorno

#### Modali BAR
```
DirectBookingModal.tsx   ← RF_BAR_11: form creazione diretta (cliente, servizio, poltrona, ora)
EditBookingModal.tsx     ← RF_BAR_12: modifica prenotazione esistente
DeleteBookingModal.tsx   ← RF_BAR_13: conferma cancellazione
```

### Attività
- [ ] Creare `DailyDashboardPage` con `useDailyDashboard` hook
- [ ] Creare `WeeklyDashboardPage` con griglia settimanale
- [ ] Implementare le 3 modali BAR
- [ ] Auto-refresh ogni 30 secondi con `refetchInterval`

---

## Fase 9.10 — Gestione Risorse BAR (Servizi, Poltrone, Orari)

**Obiettivo**: Le pagine di configurazione del salone per il BAR.

### Pagine da creare

#### `ManageServicesPage.tsx`
- Lista servizi con nome, durata, prezzo
- Toggle attivo/disattivato (soft-delete visivo)
- Azioni: Aggiungi, Modifica, Elimina
- Form in-page o in modal con validazione

#### `ManageChairsPage.tsx`
- Lista poltrone con nome e stato
- Azioni: Aggiungi, Rinomina, Disattiva
- Feedback se si tenta di creare con nome duplicato

#### `ManageSchedulesPage.tsx`
- Vista tabella: giorno × poltrona
- Per ogni cella: orari di apertura + pause
- Azioni: Aggiungi fascia, Rimuovi fascia

### Attività
- [ ] Creare le 3 pagine di gestione con CRUD completo
- [ ] Invalidazione cache TanStack Query post-mutazione
- [ ] Validazione lato client con Zod + feedback visivo

---

## Fase 9.11 — Notifiche SSE Real-time

**Obiettivo**: Notifiche push in real-time tramite SSE, con bell icon e dropdown notifiche.

### `useNotificationsSSE.ts` — Hook SSE
```typescript
export function useNotificationsSSE() {
  const { isAuthenticated } = useAuthStore();
  const notificationStore = useNotificationStore();
  const queryClient = useQueryClient();

  useEffect(() => {
    if (!isAuthenticated()) return;

    const evtSource = new EventSource('/api/notifications/stream', {
      withCredentials: true
    });

    evtSource.addEventListener('notification', (event) => {
      const notification: NotificationPushDto = JSON.parse(event.data);
      notificationStore.addUnread(notification);
      queryClient.invalidateQueries({ queryKey: ['notifications'] });
      // Mostra toast
      toast.info(notification.titolo);
    });

    evtSource.addEventListener('connected', () => {
      console.log('✅ SSE connesso');
    });

    evtSource.onerror = () => {
      evtSource.close();
      // Reconnect dopo 5 secondi
      setTimeout(() => {/* reconnect logic */}, 5000);
    };

    return () => evtSource.close();
  }, [isAuthenticated()]);
}
```

### `NotificationBell.tsx`
```
NotificationBell.tsx
  ├── icona campanella Lucide
  ├── Badge rosso con contatore non lette
  └── Dropdown con lista ultime 5 notifiche
        └── Link "Vedi tutte" → pagina notifiche
```

### Attività
- [ ] Creare `useNotificationsSSE` hook
- [ ] Creare `stores/notificationStore.ts` per badge count
- [ ] Creare `NotificationBell.tsx` con dropdown
- [ ] Avviare SSE al login, chiuderlo al logout
- [ ] Gestire reconnect automatico se la connessione cade

---

## Fase 9.12 — Test Frontend

**Obiettivo**: Verificare i componenti critici con Vitest + React Testing Library + MSW.

### MSW Handlers (per test e sviluppo offline)
```typescript
// mocks/handlers.ts
export const handlers = [
  http.post('/api/auth/login', () => {
    return HttpResponse.json({ accessToken: 'mock-token' });
  }),
  http.get('/api/availability', () => {
    return HttpResponse.json(mockAvailabilityData);
  }),
  // ... handler per ogni endpoint
];
```

### Test da implementare

**Component Test: `BookingStatusBadge.test.tsx`**
```typescript
test('renders correct color for IN_ATTESA status')
test('renders correct color for ACCETTATA status')
test('renders correct color for ANNULLATA status')
```

**Component Test: `LoginPage.test.tsx`**
```typescript
test('submit with valid data calls login mutation')
test('shows error message on invalid credentials')
test('redirects to /barber after barber login')
test('redirects to /my after client login')
```

**Component Test: `CancelBookingModal.test.tsx`**
```typescript
test('submit button disabled if reason is empty')
test('submit button enabled if reason has content')
test('calls cancelBooking mutation with correct reason')
```

**Hook Test: `useCreateBooking.test.ts`**
```typescript
test('on success, invalidates availability cache')
test('on error, does not invalidate cache')
```

**Hook Test: `useAvailableSlots.test.ts`**
```typescript
test('does not fetch if date or serviceId is null')
test('fetches with correct params when both are set')
```

### Attività
- [ ] Configurare MSW in Vitest (`setupTests.ts`)
- [ ] Creare `mocks/handlers.ts` con tutti i mock endpoint
- [ ] Implementare i component test
- [ ] Implementare i hook test

---

## Fase 9.13 — Responsive & Accessibilità

**Obiettivo**: Garantire usabilità su tutti i dispositivi e rispetto base dell'accessibilità.

### Breakpoint da verificare
| Breakpoint | Larghezza | Uso |
|------------|-----------|-----|
| Mobile | 375px | Prenotazione da telefono (CLR/CLG) |
| Tablet | 768px | Uso generale |
| Desktop | 1280px | Dashboard BAR |

### Checklist Responsive
- [ ] Navbar: hamburger menu su mobile, sidebar espansa su desktop
- [ ] Dashboard giornaliera: stack verticale su mobile, colonne su desktop
- [ ] `SlotGrid`: griglia 2 colonne su mobile, 4 su desktop
- [ ] Modali: full-screen su mobile, dialog centrato su desktop
- [ ] Form: full-width su mobile, max-width su desktop

### Checklist Accessibilità (baseline)
- [ ] Tutti gli input hanno `id` univoci e `label` associata
- [ ] Immagini con `alt` descrittivo (se presenti)
- [ ] Focus order logico con Tab
- [ ] Contrasto colori ≥ 4.5:1 (WCAG AA)
- [ ] Messaggi di errore accessibili via `aria-live="polite"`

### Attività
- [ ] Testare su viewport 375px, 768px, 1280px
- [ ] Verificare checklist accessibilità

---

## Fase 9.14 — Verifica Quality Gate

### Checklist finale Sprint 9
- [ ] Tutte le schermate implementate e funzionanti
- [ ] Flusso prenotazione CLR: dalla vetrina alla conferma
- [ ] Flusso prenotazione CLG: form dati ospite obbligatorio
- [ ] Dashboard BAR: accettazione/rifiuto prenotazioni
- [ ] SSE notifiche funzionanti in real-time
- [ ] Interceptor Axios: refresh automatico senza logout
- [ ] Responsive: funziona su 375px, 768px, 1280px
- [ ] Testi in italiano (RNF_X_1)
- [ ] Test frontend verdi (Vitest)
- [ ] Docker Compose frontend servito correttamente

---

## Definition of Done — Sprint 9

| Criterio | Verifica |
|----------|----------|
| ✅ Autenticazione completa | Login, register, logout, refresh automatico |
| ✅ Flusso prenotazione | Multi-step per CLR e CLG funzionanti |
| ✅ Dashboard BAR completa | Giornaliera + settimanale con azioni inline |
| ✅ Gestione risorse BAR | Servizi, poltrone, orari configurabili |
| ✅ Area CLR completa | Homepage, storico, profilo |
| ✅ Notifiche SSE | Push real-time + badge + dropdown |
| ✅ Responsive | 375px, 768px, 1280px verificati |
| ✅ Lingua italiana | RNF_X_1 rispettato su tutte le label |
| ✅ JWT in memoria | Access Token in Zustand, RT in cookie HttpOnly |
| ✅ Test frontend verdi | Component e hook test passano |
| ✅ CI aggiornata | GitHub Actions include build frontend |

---

*Sprint 9 — Ultima modifica: 22/04/2026*
