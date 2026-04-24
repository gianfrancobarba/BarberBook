-- Account BAR pre-configurato (non si registra autonomamente)
-- Password: 'admin1234' hashata con BCrypt strength 12
-- IMPORTANTE: cambiare in produzione
INSERT INTO users (nome, cognome, email, telefono, ruolo, created_at)
VALUES ('Tony', 'Hairman', 'tony@hairmanbarber.it', '3331234567', 'BARBER', NOW());

INSERT INTO barbers (id)
SELECT id FROM users WHERE email = 'tony@hairmanbarber.it';
