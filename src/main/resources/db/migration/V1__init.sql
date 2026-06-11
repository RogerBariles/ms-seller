CREATE TABLE users (
    id UUID PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    email VARCHAR(180) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE cash_registers (
    id UUID PRIMARY KEY,
    business_date DATE NOT NULL UNIQUE,
    initial_cash NUMERIC(12, 2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    opened_by UUID NOT NULL REFERENCES users(id),
    closed_by UUID REFERENCES users(id),
    opened_at TIMESTAMPTZ NOT NULL,
    closed_at TIMESTAMPTZ
);

CREATE TABLE shifts (
    id UUID PRIMARY KEY,
    cash_register_id UUID NOT NULL REFERENCES cash_registers(id),
    seller_id UUID NOT NULL REFERENCES users(id),
    initial_cash NUMERIC(12, 2) NOT NULL,
    cash_sales_total NUMERIC(12, 2),
    sales_count INTEGER,
    status VARCHAR(20) NOT NULL,
    started_at TIMESTAMPTZ NOT NULL,
    ended_at TIMESTAMPTZ
);

CREATE TABLE products (
    id UUID PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    category VARCHAR(20) NOT NULL,
    price NUMERIC(12, 2) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE product_price_audits (
    id UUID PRIMARY KEY,
    product_id UUID NOT NULL REFERENCES products(id),
    changed_by UUID NOT NULL REFERENCES users(id),
    old_price NUMERIC(12, 2) NOT NULL,
    new_price NUMERIC(12, 2) NOT NULL,
    change_type VARCHAR(30) NOT NULL,
    changed_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE sales (
    id UUID PRIMARY KEY,
    shift_id UUID REFERENCES shifts(id),
    seller_id UUID NOT NULL REFERENCES users(id),
    payment_method VARCHAR(20) NOT NULL,
    installments INTEGER,
    subtotal NUMERIC(12, 2) NOT NULL,
    discount_total NUMERIC(12, 2) NOT NULL,
    total NUMERIC(12, 2) NOT NULL,
    total_discount_type VARCHAR(20),
    total_discount_value NUMERIC(12, 2),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE sale_items (
    id UUID PRIMARY KEY,
    sale_id UUID NOT NULL REFERENCES sales(id) ON DELETE CASCADE,
    product_id UUID NOT NULL REFERENCES products(id),
    product_name VARCHAR(200) NOT NULL,
    quantity INTEGER NOT NULL,
    unit_price NUMERIC(12, 2) NOT NULL,
    discount_type VARCHAR(20),
    discount_value NUMERIC(12, 2),
    line_subtotal NUMERIC(12, 2) NOT NULL,
    line_discount NUMERIC(12, 2) NOT NULL DEFAULT 0,
    line_total NUMERIC(12, 2) NOT NULL
);

CREATE INDEX idx_sales_created_at ON sales(created_at);
CREATE INDEX idx_sales_payment_method ON sales(payment_method);
CREATE INDEX idx_sales_seller_id ON sales(seller_id);
CREATE INDEX idx_products_name ON products(name);
CREATE INDEX idx_products_category ON products(category);
CREATE INDEX idx_shifts_seller_status ON shifts(seller_id, status);
