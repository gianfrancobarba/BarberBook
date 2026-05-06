import { http, HttpResponse } from 'msw';

// ── Fixture dati ──────────────────────────────────────────────────────────────

const mockUser = {
  id: 1,
  nome: 'Mario',
  cognome: 'Rossi',
  email: 'mario@test.com',
  telefono: '+39 333 1234567',
  ruolo: 'CLIENT' as const,
};

const mockBarberUser = {
  id: 10,
  nome: 'Tony',
  cognome: 'Barbiere',
  email: 'tony@hairmanbarber.it',
  ruolo: 'BARBER' as const,
};

const mockServices = [
  { id: 1, nome: 'Taglio Classico', descrizione: 'Taglio tradizionale con forbici e macchinetta', durata: 30, prezzo: 18, attivo: true },
  { id: 2, nome: 'Barba', descrizione: 'Rasatura e cura della barba', durata: 20, prezzo: 12, attivo: true },
  { id: 3, nome: 'Taglio + Barba', descrizione: 'Taglio completo con rasatura barba', durata: 45, prezzo: 28, attivo: true },
  { id: 4, nome: 'Sfumatura', descrizione: 'Sfumatura con degradé', durata: 25, prezzo: 15, attivo: true },
];

const mockChairs = [
  { id: 1, nome: 'Poltrona 1', attiva: true },
  { id: 2, nome: 'Poltrona 2', attiva: true },
];

const mockBooking = {
  id: 100,
  client: mockUser,
  servizio: mockServices[0],
  poltrona: mockChairs[0],
  startTime: '2025-12-15T10:00:00',
  endTime: '2025-12-15T10:30:00',
  stato: 'ACCETTATA' as const,
  createdAt: '2025-12-10T08:00:00',
};

const mockNotifications = [
  {
    id: 1,
    tipo: 'PRENOTAZIONE_ACCETTATA',
    titolo: 'Prenotazione confermata',
    messaggio: 'La tua prenotazione per "Taglio Classico" del 15/12 alle 10:00 è stata confermata',
    letta: false,
    createdAt: '2025-12-10T09:00:00',
  },
];

// ── Handlers ──────────────────────────────────────────────────────────────────

export const handlers = [
  // Health
  http.get('/api/health', () => HttpResponse.json({ status: 'UP' })),

  // Auth
  http.post('/api/auth/login', () =>
    HttpResponse.json({
      accessToken: 'fake-jwt-token-test',
      user: mockUser,
    }),
  ),

  http.post('/api/auth/register', () =>
    HttpResponse.json(
      { accessToken: 'fake-jwt-token-new', user: { ...mockUser, id: 2, email: 'nuovo@test.com' } },
      { status: 201 },
    ),
  ),

  http.post('/api/auth/logout', () => new HttpResponse(null, { status: 200 })),

  http.post('/api/auth/refresh', () =>
    HttpResponse.json({ accessToken: 'fake-jwt-token-refreshed', user: mockUser }),
  ),

  http.post('/api/auth/forgot-password', () => new HttpResponse(null, { status: 200 })),

  http.post('/api/auth/reset-password', () => new HttpResponse(null, { status: 200 })),

  // Users
  http.get('/api/users/me', () => HttpResponse.json(mockUser)),

  http.patch('/api/users/me', async ({ request }) => {
    const body = await request.json() as Record<string, string>;
    return HttpResponse.json({ ...mockUser, ...body });
  }),

  // Services
  http.get('/api/services', () => HttpResponse.json(mockServices)),

  http.get('/api/services/:id', ({ params }) => {
    const service = mockServices.find(s => s.id === Number(params.id));
    return service ? HttpResponse.json(service) : new HttpResponse(null, { status: 404 });
  }),

  http.post('/api/services', async ({ request }) => {
    const body = await request.json() as Record<string, unknown>;
    return HttpResponse.json({ id: 99, attivo: true, ...body }, { status: 201 });
  }),

  http.patch('/api/services/:id', async ({ params, request }) => {
    const body = await request.json() as Record<string, unknown>;
    const service = mockServices.find(s => s.id === Number(params.id));
    return service ? HttpResponse.json({ ...service, ...body }) : new HttpResponse(null, { status: 404 });
  }),

  http.delete('/api/services/:id', () => new HttpResponse(null, { status: 204 })),

  // Chairs
  http.get('/api/chairs', () => HttpResponse.json(mockChairs)),

  http.get('/api/chairs/:id', ({ params }) => {
    const chair = mockChairs.find(c => c.id === Number(params.id));
    return chair ? HttpResponse.json(chair) : new HttpResponse(null, { status: 404 });
  }),

  http.post('/api/chairs', async ({ request }) => {
    const body = await request.json() as Record<string, unknown>;
    return HttpResponse.json({ id: 99, attiva: true, ...body }, { status: 201 });
  }),

  http.patch('/api/chairs/:id', async ({ params, request }) => {
    const body = await request.json() as Record<string, unknown>;
    const chair = mockChairs.find(c => c.id === Number(params.id));
    return chair ? HttpResponse.json({ ...chair, ...body }) : new HttpResponse(null, { status: 404 });
  }),

  http.delete('/api/chairs/:id', () => new HttpResponse(null, { status: 204 })),

  // Schedules
  http.get('/api/schedules/chairs/:id', () => HttpResponse.json([])),

  http.post('/api/schedules', async ({ request }) => {
    const body = await request.json() as Record<string, unknown>;
    return HttpResponse.json({ id: 1, ...body }, { status: 201 });
  }),

  http.delete('/api/schedules/:id', () => new HttpResponse(null, { status: 204 })),

  // Availability
  http.get('/api/availability', () =>
    HttpResponse.json([
      {
        chairId: 1,
        chairName: 'Poltrona 1',
        slots: [
          { startTime: '2025-12-15T09:00:00', endTime: '2025-12-15T09:30:00' },
          { startTime: '2025-12-15T10:00:00', endTime: '2025-12-15T10:30:00' },
          { startTime: '2025-12-15T11:00:00', endTime: '2025-12-15T11:30:00' },
        ],
      },
    ]),
  ),

  // Bookings
  http.post('/api/bookings', () =>
    HttpResponse.json({ ...mockBooking, id: 200, stato: 'IN_ATTESA' }, { status: 201 }),
  ),

  http.post('/api/bookings/guest', () =>
    HttpResponse.json({ ...mockBooking, id: 201, client: undefined, stato: 'IN_ATTESA' }, { status: 201 }),
  ),

  http.post('/api/bookings/direct', () =>
    HttpResponse.json({ ...mockBooking, id: 202, stato: 'ACCETTATA' }, { status: 201 }),
  ),

  http.patch('/api/bookings/:id/accept', () => new HttpResponse(null, { status: 200 })),

  http.patch('/api/bookings/:id/reject', () => new HttpResponse(null, { status: 200 })),

  http.patch('/api/bookings/:id/cancel', () => new HttpResponse(null, { status: 200 })),

  http.patch('/api/bookings/:id', async ({ request }) => {
    const body = await request.json() as Record<string, unknown>;
    return HttpResponse.json({ ...mockBooking, ...body });
  }),

  http.delete('/api/bookings/:id', () => new HttpResponse(null, { status: 204 })),

  http.get('/api/bookings/pending', () => HttpResponse.json([])),

  http.get('/api/bookings/my', () => HttpResponse.json([mockBooking])),

  http.post('/api/bookings/:id/rebook', () => HttpResponse.json(mockBooking)),

  // Client portal
  http.get('/api/client/homepage', () =>
    HttpResponse.json({
      nomeCompleto: 'Mario Rossi',
      upcomingBookings: [mockBooking],
      upcomingCount: 1,
      unreadNotifications: 1,
    }),
  ),

  http.get('/api/client/bookings', () => HttpResponse.json([mockBooking])),

  http.get('/api/client/bookings/filter', () => HttpResponse.json([mockBooking])),

  http.get('/api/client/bookings/upcoming', () => HttpResponse.json([mockBooking])),

  // Dashboard (barber)
  http.get('/api/dashboard/daily', () =>
    HttpResponse.json({
      date: '2025-12-15',
      chairs: [
        {
          chairId: 1,
          chairName: 'Poltrona 1',
          date: '2025-12-15',
          bookings: [mockBooking],
          freeSlots: [
            { start: '11:00', end: '12:00' },
            { start: '14:00', end: '17:00' },
          ],
        },
      ],
    }),
  ),

  http.get('/api/dashboard/weekly', () =>
    HttpResponse.json({
      weekStart: '2025-12-15',
      weekEnd: '2025-12-21',
      days: [
        {
          date: '2025-12-15',
          dayName: 'Lunedì',
          chairs: [
            {
              chairId: 1,
              chairName: 'Poltrona 1',
              date: '2025-12-15',
              bookings: [mockBooking],
              freeSlots: [{ start: '11:00', end: '12:00' }],
            },
          ],
        },
      ],
    }),
  ),

  // Notifications
  http.get('/api/notifications', () => HttpResponse.json(mockNotifications)),

  http.patch('/api/notifications/:id/read', () => new HttpResponse(null, { status: 200 })),

  http.patch('/api/notifications/read-all', () => new HttpResponse(null, { status: 200 })),
];
