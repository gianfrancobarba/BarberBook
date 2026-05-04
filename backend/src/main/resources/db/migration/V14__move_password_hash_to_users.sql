-- V14: Sposta password_hash da clients a users per permettere il login ai barbieri
ALTER TABLE users ADD COLUMN password_hash VARCHAR(255);

-- Se ci sono dati in clients, li spostiamo (anche se in questo momento dovrebbero essere solo test)
UPDATE users u
SET password_hash = c.password_hash
FROM clients c
WHERE u.id = c.id;

ALTER TABLE clients DROP COLUMN password_hash;

-- Aggiorna il barbiere seedato con la password 'admin1234'
-- Hash BCrypt strength 12 per 'admin1234'
UPDATE users 
SET password_hash = '$2a$12$0Nl.lR/wz6P0S/g6Gg/P7.1r58yI7g4yV0xS40mSg1/m.s23o20L6'
WHERE email = 'tony@hairmanbarber.it';
