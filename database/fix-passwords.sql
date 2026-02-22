-- Admin-Passwort aktualisieren (admin123)
UPDATE users
SET password_hash = '$2a$12$MMbkxZQfQePt3aApd8bCsuSv0U7pT54rR708XyXXNq9gcnfjrsTBy'
WHERE username = 'admin';

-- Testbenutzer aktualisieren (password123)
UPDATE users
SET password_hash = '$2a$12$9qf4aU3aQ.iXkYJYAea3deFQODQxKIwpV63Vz7p6CuCya.s696RXG'
WHERE username IN ('jsmith', 'mwilson', 'tjohnson');

-- Aktualisierungen überprüfen
SELECT username, email, role, substring(password_hash, 1, 30) as hash_prefix
FROM users
ORDER BY username;
