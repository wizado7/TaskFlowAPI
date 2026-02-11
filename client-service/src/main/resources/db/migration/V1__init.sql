CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE clients (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255),
    phone VARCHAR(100),
    company VARCHAR(255),
    notes TEXT
);

CREATE INDEX idx_clients_email ON clients(email);
