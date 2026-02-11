CREATE TABLE IF NOT EXISTS user_notifications (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_email VARCHAR(255) NOT NULL,
    title VARCHAR(160) NOT NULL,
    message TEXT NOT NULL,
    type VARCHAR(40) NOT NULL,
    action_url VARCHAR(255),
    action_label VARCHAR(80),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    is_read BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_user_notifications_email ON user_notifications(user_email);
CREATE INDEX IF NOT EXISTS idx_user_notifications_read ON user_notifications(is_read);
