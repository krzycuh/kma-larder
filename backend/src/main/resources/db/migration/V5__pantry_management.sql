-- Pantries are managed by their users now: registration no longer creates one, a user creates, deletes and
-- picks the default pantry on their own.

-- The pantry a user's pages open on. At most one per user; without one the oldest pantry is used.
ALTER TABLE pantry_members ADD COLUMN is_default BOOLEAN NOT NULL DEFAULT false;
CREATE UNIQUE INDEX ux_pantry_members_default ON pantry_members(user_id) WHERE is_default;

-- Leftovers of "every account gets a pantry": a pantry nobody ever used (no products, nobody but the owner)
-- whose owner belongs to another household's pantry. It only got in the way of the shared one.
DELETE FROM pantries p
WHERE NOT EXISTS (SELECT 1 FROM products pr WHERE pr.pantry_id = p.id)
  AND NOT EXISTS (SELECT 1 FROM pantry_members m WHERE m.pantry_id = p.id AND m.user_id <> p.owner_id)
  AND EXISTS (SELECT 1 FROM pantry_members m WHERE m.user_id = p.owner_id AND m.pantry_id <> p.id);
