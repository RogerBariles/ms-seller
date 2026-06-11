ALTER TABLE users RENAME COLUMN email TO username;

ALTER TABLE users ADD COLUMN birth_date DATE;

UPDATE users SET username = 'admin' WHERE username = 'admin@pasteleria.com';

ALTER TABLE sales ADD COLUMN birthday BOOLEAN NOT NULL DEFAULT FALSE;

INSERT INTO products (id, name, category, price, active) VALUES
    ('20000000-0000-0000-0000-000000000001', 'Cortesía cumpleaños', 'COTILLON', 0.00, TRUE)
ON CONFLICT (id) DO NOTHING;
