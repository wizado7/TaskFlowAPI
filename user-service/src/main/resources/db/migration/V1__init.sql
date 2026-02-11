CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE IF NOT EXISTS user_profiles (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_email VARCHAR(255) NOT NULL UNIQUE,
    full_name VARCHAR(120),
    phone VARCHAR(30),
    timezone VARCHAR(60),
    avatar_url VARCHAR(255)
);
