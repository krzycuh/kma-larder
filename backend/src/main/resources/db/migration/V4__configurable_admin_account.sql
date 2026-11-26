-- The seeded administrator (V2) gets the address and name from the environment: APP_ADMIN_EMAIL and APP_ADMIN_NAME,
-- passed in as Flyway placeholders (spring.flyway.placeholders.*), so a household does not stay with admin@example.com.
-- Runs once, like every migration: on a fresh install right after V2, on an existing database at the next start.
-- The account is renamed only while it still carries the seeded address and the new one is free; the password stays.
UPDATE users
SET email = trim('${admin-email}'),
    updated_at = CURRENT_TIMESTAMP
WHERE email = 'admin@example.com'
  AND trim('${admin-email}') <> ''
  AND trim('${admin-email}') <> 'admin@example.com'
  AND NOT EXISTS (SELECT 1 FROM users WHERE email = trim('${admin-email}'));

UPDATE users
SET name = trim('${admin-name}'),
    updated_at = CURRENT_TIMESTAMP
WHERE email = trim('${admin-email}')
  AND name = 'Admin'
  AND trim('${admin-name}') <> ''
  AND trim('${admin-name}') <> 'Admin';

-- The default pantry follows the owner's name while it still has the seeded one
UPDATE pantries p
SET name = 'Spiżarnia ' || u.name,
    updated_at = CURRENT_TIMESTAMP
FROM users u
WHERE u.id = p.owner_id
  AND u.email = trim('${admin-email}')
  AND p.name = 'Spiżarnia Admin'
  AND u.name <> 'Admin';
