-- Initial admin user
-- Default password: "admin123" (BCrypt). Change it after first login!
INSERT INTO users (id, email, password_hash, name, role, created_at, updated_at)
VALUES (
    gen_random_uuid(),
    'admin@example.com',
    '$2a$10$R8JmlnGD9G05IfJYZtZp.elgSgck/iKyc22kjJDyNYoQWbUqaeZNS',
    'Admin',
    'ADMIN',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (email) DO NOTHING;

-- Default pantry for the admin user
INSERT INTO pantries (id, name, owner_id)
SELECT gen_random_uuid(), 'Spiżarnia Admin', u.id
FROM users u
WHERE u.email = 'admin@example.com'
  AND NOT EXISTS (SELECT 1 FROM pantries p WHERE p.owner_id = u.id);

INSERT INTO pantry_members (pantry_id, user_id, role)
SELECT p.id, p.owner_id, 'OWNER'
FROM pantries p
JOIN users u ON u.id = p.owner_id
WHERE u.email = 'admin@example.com'
ON CONFLICT DO NOTHING;
