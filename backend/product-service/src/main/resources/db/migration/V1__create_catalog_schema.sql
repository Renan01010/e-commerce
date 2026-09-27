CREATE TABLE categories (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(500),
    parent_category_id UUID REFERENCES categories(id) ON DELETE SET NULL,
    display_order INTEGER NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    created_by VARCHAR(100) NOT NULL,
    updated_by VARCHAR(100) NOT NULL,
    CONSTRAINT chk_category_name_not_empty CHECK (length(btrim(name)) > 0)
);

CREATE INDEX idx_categories_parent_category_id ON categories(parent_category_id) WHERE parent_category_id IS NOT NULL;
CREATE INDEX idx_categories_name_active ON categories(name) WHERE is_active = TRUE;

CREATE TABLE products (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    price NUMERIC(12, 2) NOT NULL,
    cost NUMERIC(12, 2),
    brand VARCHAR(100),
    sku VARCHAR(50) NOT NULL UNIQUE,
    category_id UUID NOT NULL REFERENCES categories(id) ON DELETE RESTRICT,
    quantity INTEGER NOT NULL DEFAULT 0,
    image_url VARCHAR(2000),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    created_by VARCHAR(100) NOT NULL,
    updated_by VARCHAR(100) NOT NULL,
    CONSTRAINT chk_product_name_not_empty CHECK (length(btrim(name)) > 0),
    CONSTRAINT chk_product_price_non_negative CHECK (price >= 0),
    CONSTRAINT chk_product_cost_non_negative CHECK (cost IS NULL OR cost >= 0),
    CONSTRAINT chk_product_quantity_non_negative CHECK (quantity >= 0),
    CONSTRAINT uk_product_name_category UNIQUE (name, category_id)
);

ALTER TABLE products ADD COLUMN search_vector tsvector GENERATED ALWAYS AS (
    setweight(to_tsvector('english', coalesce(name, '')), 'A') ||
    setweight(to_tsvector('english', coalesce(brand, '')), 'B') ||
    setweight(to_tsvector('english', coalesce(description, '')), 'C')
) STORED;

CREATE INDEX idx_products_search_fts ON products USING GIN(search_vector);
CREATE INDEX idx_products_category_id ON products(category_id);
CREATE INDEX idx_products_active_created_at ON products(is_active, created_at DESC);
CREATE INDEX idx_products_price_active ON products(price, is_active);
CREATE INDEX idx_products_quantity_active ON products(quantity, is_active);