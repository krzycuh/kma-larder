-- KMA Larder – initial schema
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- ============================================
-- Identity & Access
-- ============================================
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL CHECK (role IN ('USER', 'ADMIN')),
    avatar_url VARCHAR(500),
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_users_role ON users(role);

-- ============================================
-- Pantry (household pantries and their members)
-- ============================================
CREATE TABLE pantries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    owner_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_pantries_owner ON pantries(owner_id);

CREATE TABLE pantry_members (
    pantry_id UUID NOT NULL REFERENCES pantries(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role VARCHAR(20) NOT NULL CHECK (role IN ('OWNER', 'MEMBER')),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (pantry_id, user_id)
);

CREATE INDEX idx_pantry_members_user ON pantry_members(user_id);

-- ============================================
-- Stock: products (per pantry), batches, movements
-- ============================================
-- A product is what the household keeps: "Ogórki kiszone", "Mąka pszenna 1 kg". It belongs to one pantry.
CREATE TABLE products (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    pantry_id UUID NOT NULL REFERENCES pantries(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    normalized_name VARCHAR(255) NOT NULL,
    kind VARCHAR(20) NOT NULL CHECK (kind IN ('STORE_BOUGHT', 'HOMEMADE')),
    category VARCHAR(30) NOT NULL,
    barcode VARCHAR(14),
    package_size VARCHAR(50),
    default_shelf_life_months INTEGER CHECK (default_shelf_life_months IS NULL OR default_shelf_life_months > 0),
    min_quantity INTEGER NOT NULL DEFAULT 0 CHECK (min_quantity >= 0),
    notes TEXT,
    archived BOOLEAN NOT NULL DEFAULT false,
    created_by_user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX ux_products_pantry_barcode ON products(pantry_id, barcode) WHERE barcode IS NOT NULL;
CREATE INDEX idx_products_pantry_name ON products(pantry_id, normalized_name);
CREATE INDEX idx_products_name_trgm ON products USING gin (name gin_trgm_ops);

-- A batch is a number of identical units with the same date: 6 jars of 2026 cucumbers, 3 cans best before 05.2028.
-- Dates have year precision with optional month and day. expires_on is the last day of the best-before period
-- (given, or derived from produced_* + the product's shelf life), kept here so lists can sort and filter in SQL.
CREATE TABLE batches (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    pantry_id UUID NOT NULL REFERENCES pantries(id) ON DELETE CASCADE,
    product_id UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    quantity INTEGER NOT NULL CHECK (quantity >= 0),
    best_before_year SMALLINT,
    best_before_month SMALLINT CHECK (best_before_month BETWEEN 1 AND 12),
    best_before_day SMALLINT CHECK (best_before_day BETWEEN 1 AND 31),
    produced_year SMALLINT,
    produced_month SMALLINT CHECK (produced_month BETWEEN 1 AND 12),
    produced_day SMALLINT CHECK (produced_day BETWEEN 1 AND 31),
    expires_on DATE,
    location VARCHAR(100),
    notes TEXT,
    added_on DATE NOT NULL DEFAULT CURRENT_DATE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK (best_before_month IS NULL OR best_before_year IS NOT NULL),
    CHECK (best_before_day IS NULL OR best_before_month IS NOT NULL),
    CHECK (produced_month IS NULL OR produced_year IS NOT NULL),
    CHECK (produced_day IS NULL OR produced_month IS NOT NULL)
);

CREATE INDEX idx_batches_pantry_expires ON batches(pantry_id, expires_on);
CREATE INDEX idx_batches_product ON batches(product_id, expires_on);

-- Every change of a batch's quantity: what was added, eaten or thrown away and when ("how many jars a year").
CREATE TABLE stock_movements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    pantry_id UUID NOT NULL REFERENCES pantries(id) ON DELETE CASCADE,
    product_id UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    batch_id UUID REFERENCES batches(id) ON DELETE SET NULL,
    type VARCHAR(20) NOT NULL CHECK (type IN ('ADDED', 'CONSUMED', 'DISCARDED', 'ADJUSTED')),
    quantity INTEGER NOT NULL,
    occurred_on DATE NOT NULL DEFAULT CURRENT_DATE,
    user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_stock_movements_pantry ON stock_movements(pantry_id, occurred_on DESC);
CREATE INDEX idx_stock_movements_product ON stock_movements(product_id, occurred_on DESC);

-- ============================================
-- updated_at trigger
-- ============================================
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_users_updated_at BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_pantries_updated_at BEFORE UPDATE ON pantries
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_products_updated_at BEFORE UPDATE ON products
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_batches_updated_at BEFORE UPDATE ON batches
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
