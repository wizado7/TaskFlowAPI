CREATE TABLE IF NOT EXISTS oauth_login_codes (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    code_hash VARCHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    consumed_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_oauth_login_codes_user_id ON oauth_login_codes(user_id);
CREATE INDEX IF NOT EXISTS idx_oauth_login_codes_expires_at ON oauth_login_codes(expires_at);
