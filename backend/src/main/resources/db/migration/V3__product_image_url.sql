-- A link to a small product photo (from Open Food Facts or typed in); the image itself is not stored here.
ALTER TABLE products ADD COLUMN image_url VARCHAR(500);
