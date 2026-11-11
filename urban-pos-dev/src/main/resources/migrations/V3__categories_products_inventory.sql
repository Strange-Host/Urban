CREATE TABLE categories (
                            id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                            name VARCHAR(100) UNIQUE NOT NULL,
                            description TEXT,

                            created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                            updated_at TIMESTAMPTZ,
                            deleted_at TIMESTAMPTZ
);

CREATE TABLE products (
                          id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),

                          name VARCHAR(255) NOT NULL,
                          sku VARCHAR(100) UNIQUE,

                          selling_price NUMERIC(14,2) NOT NULL,
                          cost_price NUMERIC(14,2) NOT NULL,

                          category_id UUID NOT NULL REFERENCES categories(id),

                          status VARCHAR(50),

                          created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                          updated_at TIMESTAMPTZ,
                          deleted_at TIMESTAMPTZ
);

CREATE TABLE inventory (
                           id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),

                           product_id UUID NOT NULL UNIQUE REFERENCES products(id) ON DELETE CASCADE,

                           quantity NUMERIC(14,2) DEFAULT 0,
                           reorder_level NUMERIC(14,2) DEFAULT 0,

                           created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                           updated_at TIMESTAMPTZ,
                           deleted_at TIMESTAMPTZ
);
