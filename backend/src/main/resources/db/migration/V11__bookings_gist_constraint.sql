-- Abilita l'estensione btree_gist (necessaria per GiST con type range + eguaglianza)
CREATE EXTENSION IF NOT EXISTS btree_gist;

-- Exclusion Constraint con GiST: nessuna prenotazione può sovrapporsi
-- sulla stessa poltrona se entrambe sono IN_ATTESA o ACCETTATA.
-- Questo garantisce il no-double-booking a livello fisico, immune da race condition.
ALTER TABLE bookings
ADD CONSTRAINT no_overlap_booking
EXCLUDE USING GIST (
    chair_id WITH =,
    tsrange(start_time, end_time, '[)') WITH &&
)
WHERE (status IN ('IN_ATTESA', 'ACCETTATA'));
