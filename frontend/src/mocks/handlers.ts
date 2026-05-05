import { http, HttpResponse } from 'msw';

export const handlers = [
  // Aggiungi qui gli handlers per le API
  http.get('/api/health', () => HttpResponse.json({ status: 'UP' })),
  
  http.post('/api/auth/login', () => {
    return HttpResponse.json({
      accessToken: 'fake-jwt-token',
      user: {
        id: 1,
        nome: 'Mario',
        cognome: 'Rossi',
        email: 'mario@test.com',
        ruolo: 'CLIENT'
      }
    });
  }),

  http.get('/api/services', () => {
    return HttpResponse.json([
      { id: 1, nome: 'Taglio', descrizione: 'Taglio classico', prezzo: 20, durata: 30, attivo: true }
    ]);
  }),
];
