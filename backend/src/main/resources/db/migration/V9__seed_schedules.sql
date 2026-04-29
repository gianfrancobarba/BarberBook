-- Orari di apertura: Lunedì-Sabato, 9:00-19:00, per entrambe le poltrone
-- Pausa pranzo: 13:00-15:00
-- Domenica: chiuso (nessuna fascia APERTURA = giorno chiuso)

-- Poltrona 1 (id=1): orari di apertura
INSERT INTO schedules (chair_id, giorno_settimana, ora_inizio, ora_fine, tipo)
SELECT 1, day, '09:00', '19:00', 'APERTURA'
FROM (VALUES ('MONDAY'),('TUESDAY'),('WEDNESDAY'),('THURSDAY'),('FRIDAY'),('SATURDAY')) AS t(day);

-- Poltrona 1: pausa pranzo
INSERT INTO schedules (chair_id, giorno_settimana, ora_inizio, ora_fine, tipo)
SELECT 1, day, '13:00', '15:00', 'PAUSA'
FROM (VALUES ('MONDAY'),('TUESDAY'),('WEDNESDAY'),('THURSDAY'),('FRIDAY'),('SATURDAY')) AS t(day);

-- Poltrona 2 (id=2): stessa configurazione
INSERT INTO schedules (chair_id, giorno_settimana, ora_inizio, ora_fine, tipo)
SELECT 2, day, '09:00', '19:00', 'APERTURA'
FROM (VALUES ('MONDAY'),('TUESDAY'),('WEDNESDAY'),('THURSDAY'),('FRIDAY'),('SATURDAY')) AS t(day);

INSERT INTO schedules (chair_id, giorno_settimana, ora_inizio, ora_fine, tipo)
SELECT 2, day, '13:00', '15:00', 'PAUSA'
FROM (VALUES ('MONDAY'),('TUESDAY'),('WEDNESDAY'),('THURSDAY'),('FRIDAY'),('SATURDAY')) AS t(day);
