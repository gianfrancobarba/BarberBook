import { http, HttpResponse } from 'msw';

export const handlers = [
  // Aggiungi qui gli handlers per le API
  http.get('/api/health', () => {
    return HttpResponse.json({ status: 'UP' });
  }),
];
